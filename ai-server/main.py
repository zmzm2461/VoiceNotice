from fastapi import FastAPI, UploadFile, File, WebSocket, WebSocketDisconnect
from pydantic import BaseModel
from typing import Optional
import tempfile
import os
import json
import re
import asyncio
import base64
import websockets

from faster_whisper import WhisperModel
from openai import OpenAI

app = FastAPI()

# ======================
# Whisper 모델 로드
# ======================

model = WhisperModel(
    "base",
    device="cpu",
    compute_type="int8"
)

# ======================
# OpenAI 설정
# ======================

client = OpenAI(
    api_key=os.getenv("OPENAI_API_KEY")
)

# ======================
# OpenAI Realtime STT 설정
# ======================

OPENAI_REALTIME_URL = (
    "wss://api.openai.com/v1/realtime"
    "?model=gpt-live-transcribe"
)

REALTIME_STT_SESSION_CONFIG = {
    "type": "session.update",
    "session": {
        "type": "transcription",
        "audio": {
            "input": {
                "format": {
                    "type": "audio/pcm",
                    "rate": 24000
                },
                "transcription": {
                    "model": "gpt-live-transcribe",

                    # 어떤 환경의 음성인지 알려줌
                    "prompt": (
                        "아파트 인터폰에서 방문자가 말하는 한국어 음성입니다. "
                        "택배, 배달, 관리사무소, 경비실, 점검, "
                        "설치 기사, 수리 기사 등의 표현이 자주 등장합니다."
                    ),

                    # 자주 등장하는 중요 단어
                    "keywords": [
                        "택배",
                        "배달",
                        "문 앞",
                        "관리사무소",
                        "관리실",
                        "경비실",
                        "소방 점검",
                        "도시가스",
                        "도시가스 점검",
                        "인터넷 설치",
                        "설치 기사",
                        "에어컨",
                        "에어컨 수리",
                        "서비스센터",
                        "누수",
                        "아래층"
                    ],

                    # 한국어
                    "languages": ["ko"],

                    # 속도 우선
                    "delay": "low"
                },

                # 우선 1단계에서는 자동 발화 종료 감지 사용 X
                # 아두이노가 나중에 직접 commit을 보내도록 한다.
                "turn_detection": None
            }
        }
    }
}


# ======================
# 교정 사전
# ======================

def apply_correction_dictionary(text: str) -> str:
    correction_map = {
        # 택배/배송
        "특별 왔습니다": "택배 왔습니다",
        "특별왔습니다": "택배 왔습니다",
        "특별 왔어요": "택배 왔어요",
        "특별왔어요": "택배 왔어요",
        "택패 왔습니다": "택배 왔습니다",
        "택배 와 씁니다": "택배 왔습니다",
        "택배왔 씁니다": "택배 왔습니다",
        "택배 왔 씁니다": "택배 왔습니다",
        "택배 기사입니다": "택배기사입니다",

        # 배달
        "달 왔습니다": "배달 왔습니다",
        "달왔습니다": "배달 왔습니다",
        "달 왔어요": "배달 왔어요",
        "달왔어요": "배달 왔어요",
        "배달 와 씁니다": "배달 왔습니다",
        "배달왔 씁니다": "배달 왔습니다",
        "배달 왔 씁니다": "배달 왔습니다",
        "음식 배달왔습니다": "음식 배달 왔습니다",

        # 문 앞/배송 완료
        "문압": "문 앞",
        "문 압": "문 앞",
        "문앞": "문 앞",
        "문압에": "문 앞에",
        "문 앞에 둘게여": "문 앞에 둘게요",
        "문앞에 둘게여": "문 앞에 둘게요",
        "문 앞에 두고 갈께요": "문 앞에 두고 갈게요",
        "두고 갈께요": "두고 갈게요",
        "놓고 갈께요": "놓고 갈게요",

        # 관리사무소/경비실
        "관리 소입니다": "관리사무소입니다",
        "관리소입니다": "관리사무소입니다",
        "관리 사무소입니다": "관리사무소입니다",
        "관리 소": "관리사무소",
        "관리 실입니다": "관리실입니다",
        "관리실 입니다": "관리실입니다",
        "경비 실입니다": "경비실입니다",
        "경비실 입니다": "경비실입니다",
        "경비 실": "경비실",

        # 점검
        "정검": "점검",
        "소방 정검": "소방 점검",
        "가스 정검": "가스 점검",
        "시설 정검": "시설 점검",
        "정기 정검": "정기 점검",
        "점검 나왓습니다": "점검 나왔습니다",
        "점검 나왔 습니다": "점검 나왔습니다",

        # 도시가스
        "도시 까스": "도시가스",
        "도시가 쓰": "도시가스",
        "도시 가스": "도시가스",
        "도시가스 정검": "도시가스 점검",

        # 설치/수리 기사
        "인터넷 설 치": "인터넷 설치",
        "인터넷 설치기사": "인터넷 설치 기사",
        "설치 기삽니다": "설치 기사입니다",
        "수리 기삽니다": "수리 기사입니다",
        "에컨": "에어컨",
        "어컨": "에어컨",
        "애어컨": "에어컨",
        "에어 콘": "에어컨",
        "에어컨 수리왔습니다": "에어컨 수리 왔습니다",
        "에어컨 수리 왓습니다": "에어컨 수리 왔습니다",
        "서비스 센터입니다": "서비스센터입니다",

        # 누수/긴급
        "누 수": "누수",
        "아랫층": "아래층",
        "아래 층": "아래층",
        "물이 샙니다": "물이 새고 있습니다",
        "물이 세고 있습니다": "물이 새고 있습니다",
        "긴급이 확인": "긴급히 확인",
        "긴급 히": "긴급히",
    }

    corrected = text

    for wrong, right in correction_map.items():
        corrected = corrected.replace(wrong, right)

    return corrected


def remove_stutter_noise(text: str) -> str:
    """
    STT가 만든 의미 없는 반복 발화 제거.
    예:
    자. 자. 자. 자.
    어. 어. 어.
    음. 음. 음.
    아아아아
    """

    # 자. 자. 자.
    text = re.sub(r"(자[\.\s]*){2,}", "", text)

    # 어. 어. 어.
    text = re.sub(r"(어[\.\s]*){2,}", "", text)

    # 음. 음. 음.
    text = re.sub(r"(음[\.\s]*){2,}", "", text)

    # 아. 아. 아.
    text = re.sub(r"(아[\.\s]*){2,}", "", text)

    # 네. 네. 네.
    text = re.sub(r"(네[\.\s]*){2,}", "네", text)

    # 예. 예. 예.
    text = re.sub(r"(예[\.\s]*){2,}", "예", text)

    # 같은 한글 글자가 4번 이상 반복될 때 줄임
    # 예: 자자자자자 -> 자
    text = re.sub(r"([가-힣])\1{3,}", r"\1", text)

    # 공백 정리
    text = " ".join(text.split())

    return text.strip()


def guess_category(text: str) -> str:
    if any(word in text for word in ["화재", "응급", "위험", "긴급", "가스 냄새", "누수", "물이 새"]):
        return "EMERGENCY"

    if any(word in text for word in ["택배", "배달", "음식", "우편", "소포", "배송"]):
        return "DELIVERY"

    if any(word in text for word in ["관리사무소", "관리실", "경비실", "점검", "소방", "가스", "안내"]):
        return "NOTICE"

    if any(word in text for word in ["인터넷", "설치", "수리", "에어컨", "서비스센터", "기사"]):
        return "VISITOR"

    if any(word in text for word in ["친구", "가족", "손님", "방문"]):
        return "VISITOR"

    return "ETC"


def make_simple_summary(text: str) -> str:
    category = guess_category(text)

    if category == "DELIVERY":
        if "택배" in text:
            return "택배 도착"
        if "배달" in text or "음식" in text:
            return "배달 도착"
        if "우편" in text:
            return "우편물 도착"
        return "배송 도착"

    if category == "NOTICE":
        if "소방" in text and "점검" in text:
            return "소방 점검"
        if "가스" in text and "점검" in text:
            return "가스 점검"
        if "점검" in text:
            return "점검 방문"
        if "관리사무소" in text:
            return "관리 안내"
        return "공지 안내"

    if category == "EMERGENCY":
        if "누수" in text or "물이 새" in text:
            return "누수 확인"
        if "화재" in text:
            return "화재 상황"
        if "가스 냄새" in text:
            return "가스 위험"
        return "긴급 상황"

    if category == "VISITOR":
        if "인터넷" in text and "설치" in text:
            return "인터넷 설치"
        if "에어컨" in text and "수리" in text:
            return "에어컨 수리"
        if "서비스센터" in text:
            return "서비스 방문"
        return "방문자 도착"

    return text[:20] + ("..." if len(text) > 20 else "")


def extract_json(text: str) -> dict:
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, re.DOTALL)
        if match:
            return json.loads(match.group())
        raise


# ======================
# Realtime STT 관련
# ======================

@app.websocket("/ws/stt")
async def realtime_stt(websocket: WebSocket):

    # 1. 우리 서버에 들어온 WebSocket 연결 수락
    await websocket.accept()

    print("[Realtime STT] Client connected")

    api_key = os.getenv("OPENAI_API_KEY")

    if not api_key:
        await websocket.send_json({
            "type": "error",
            "message": "OPENAI_API_KEY가 설정되어 있지 않습니다."
        })

        await websocket.close(code=1011)
        return

    try:

        # 2. Python 서버 -> OpenAI Realtime WebSocket 연결
        async with websockets.connect(
            OPENAI_REALTIME_URL,
            additional_headers={
                "Authorization": f"Bearer {api_key}"
            }
        ) as openai_ws:

            print("[Realtime STT] OpenAI connected")

            # 3. STT 세션 설정 전달
            await openai_ws.send(
                json.dumps(
                    REALTIME_STT_SESSION_CONFIG,
                    ensure_ascii=False
                )
            )

            print("[Realtime STT] Session configuration sent")

            # -------------------------------------
            # 클라이언트 -> OpenAI
            # -------------------------------------

            async def client_to_openai():

                try:
                    while True:

                        message = await websocket.receive()

                        # WebSocket 연결 종료
                        if message["type"] == "websocket.disconnect":
                            print("[Realtime STT] Client disconnected")
                            return

                        # =========================
                        # PCM 오디오를 받은 경우
                        # =========================

                        audio_bytes = message.get("bytes")

                        if audio_bytes:

                            # OpenAI에는 Base64로 보내야 함
                            audio_base64 = base64.b64encode(
                                audio_bytes
                            ).decode("ascii")

                            event = {
                                "type": "input_audio_buffer.append",
                                "audio": audio_base64
                            }

                            await openai_ws.send(
                                json.dumps(event)
                            )

                        # =========================
                        # 제어 메시지를 받은 경우
                        # =========================

                        text_message = message.get("text")

                        if text_message:

                            try:
                                command = json.loads(text_message)

                            except json.JSONDecodeError:
                                print(
                                    "[Realtime STT] "
                                    "Invalid control message:",
                                    text_message
                                )
                                continue

                            command_type = command.get("type")

                            # 발화가 끝났다는 의미
                            if command_type == "commit":

                                print(
                                    "[Realtime STT] "
                                    "Commit requested"
                                )

                                await openai_ws.send(
                                    json.dumps({
                                        "type":
                                            "input_audio_buffer.commit"
                                    })
                                )

                            # 현재 오디오 버퍼 삭제
                            elif command_type == "clear":

                                await openai_ws.send(
                                    json.dumps({
                                        "type":
                                            "input_audio_buffer.clear"
                                    })
                                )

                except WebSocketDisconnect:
                    print(
                        "[Realtime STT] "
                        "Client WebSocket disconnected"
                    )


            # -------------------------------------
            # OpenAI -> 클라이언트
            # -------------------------------------

            async def openai_to_client():

                async for raw_message in openai_ws:

                    event = json.loads(raw_message)

                    event_type = event.get("type")

                    # =========================
                    # 중간 STT
                    # =========================

                    if (
                        event_type ==
                        "conversation.item."
                        "input_audio_transcription.delta"
                    ):

                        delta = event.get("delta", "")

                        if delta:

                            print(
                                "[Realtime partial]",
                                delta
                            )

                            await websocket.send_json({
                                "type": "partial",
                                "text": delta,
                                "itemId": event.get(
                                    "item_id"
                                )
                            })

                    # =========================
                    # 최종 STT
                    # =========================

                    elif (
                        event_type ==
                        "conversation.item."
                        "input_audio_transcription.completed"
                    ):

                        transcript = event.get(
                            "transcript",
                            ""
                        )

                        print(
                            "[Realtime final]",
                            transcript
                        )

                        await websocket.send_json({
                            "type": "final",
                            "text": transcript,
                            "itemId": event.get(
                                "item_id"
                            )
                        })

                    # =========================
                    # OpenAI 오류
                    # =========================

                    elif event_type == "error":

                        print(
                            "[Realtime STT OpenAI Error]",
                            json.dumps(
                                event,
                                ensure_ascii=False
                            )
                        )

                        await websocket.send_json({
                            "type": "error",
                            "message": event
                                .get("error", {})
                                .get(
                                    "message",
                                    "OpenAI Realtime 오류"
                                )
                        })

                    # =========================
                    # 세션 설정 완료
                    # =========================

                    elif event_type == "session.updated":

                        print(
                            "[Realtime STT] "
                            "OpenAI session ready"
                        )

                        await websocket.send_json({
                            "type": "ready"
                        })


            # =====================================
            # 양방향 처리를 동시에 실행
            # =====================================

            client_task = asyncio.create_task(
                client_to_openai()
            )

            openai_task = asyncio.create_task(
                openai_to_client()
            )

            done, pending = await asyncio.wait(
                [
                    client_task,
                    openai_task
                ],
                return_when=asyncio.FIRST_COMPLETED
            )

            # 한쪽 연결이 끝나면 나머지도 종료
            for task in pending:
                task.cancel()

            await asyncio.gather(
                *pending,
                return_exceptions=True
            )

    except Exception as e:

        print(
            "[Realtime STT Error]",
            repr(e)
        )

        try:
            await websocket.send_json({
                "type": "error",
                "message": str(e)
            })
        except Exception:
            pass

    finally:

        try:
            await websocket.close()
        except Exception:
            pass

        print(
            "[Realtime STT] Connection closed"
        )

# ======================
# STT 관련
# ======================

class SttResponse(BaseModel):
    rawText: str
    language: Optional[str] = None


@app.post("/stt", response_model=SttResponse)
async def stt(file: UploadFile = File(...)):
    suffix = os.path.splitext(file.filename)[1] if file.filename else ".wav"

    tmp = tempfile.NamedTemporaryFile(
        delete=False,
        suffix=suffix
    )

    try:
        content = await file.read()

        print("uploaded filename:", file.filename)
        print("uploaded size:", len(content))

        tmp.write(content)
        tmp.close()

        print("temp file:", tmp.name)
        print("temp file size:", os.path.getsize(tmp.name))

        segments, info = model.transcribe(
            tmp.name,
            beam_size=5,
            language="ko",
            vad_filter=False
        )

        segment_list = list(segments)

        print("segment count:", len(segment_list))

        for seg in segment_list:
            print("segment:", seg.start, seg.end, seg.text)

        text = "".join([seg.text for seg in segment_list]).strip()

        print("STT text:", text)

        return SttResponse(
            rawText=text,
            language=getattr(info, "language", None)
        )

    finally:
        try:
            os.unlink(tmp.name)
        except Exception:
            pass


# ======================
# Refine 관련
# ======================

class RefineRequest(BaseModel):
    rawText: str
    style: Optional[str] = "apartment_intercom"
    maxLength: Optional[int] = 500


class RefineResponse(BaseModel):
    finalText: str
    summary: Optional[str] = None
    category: Optional[str] = None
    confidence: Optional[float] = 1.0


@app.get("/")
def health():
    return {"status": "ok"}


@app.post("/refine", response_model=RefineResponse)
def refine(req: RefineRequest):
    raw_text = " ".join(req.rawText.strip().split())
    raw_text = remove_stutter_noise(raw_text)

    if not raw_text:
        return RefineResponse(
            finalText="내용 없음",
            summary="내용 없음",
            category="ETC",
            confidence=0.0
        )

    corrected_text = apply_correction_dictionary(raw_text)
    corrected_text = remove_stutter_noise(corrected_text)

    if not corrected_text:
        return RefineResponse(
            finalText="내용 없음",
            summary="내용 없음",
            category="ETC",
            confidence=0.0
        )

    if req.maxLength and len(corrected_text) > req.maxLength:
        corrected_text = corrected_text[:req.maxLength] + "..."

    prompt = f"""
너는 아파트 인터폰 STT 문장을 자연스럽게 다듬는 후처리 AI다.

이미 1차 오인식 교정과 의미 없는 반복 발화 제거는 완료되었다.
너는 아래 문장을 과하게 바꾸지 말고, 사용자가 읽기 좋게만 정리한다.

중요 규칙:
1. 원문에 없는 내용을 추가하지 않는다.
2. 의미를 추측해서 새로 만들지 않는다.
3. 말투, 띄어쓰기, 조사, 어미만 자연스럽게 다듬는다.
4. 문장이 이미 자연스러우면 거의 그대로 둔다.
5. 확실하지 않으면 억지로 바꾸지 않는다.
6. 광고문이나 안내방송처럼 과하게 꾸미지 않는다.
7. 짧은 문장은 짧게 유지한다.
8. 다만 아파트 인터폰 환경에서 자주 나오는 표현과 발음상 매우 유사하면 자연스럽게 교정한다.
9. 교정이 애매하면 confidence를 낮게 준다.
10. 의미 없는 반복 발화(자 자 자, 어 어 어, 음 음 음)는 제거한다.

인터폰 환경에서 자주 등장하는 표현 후보:
- 택배 왔습니다
- 배달 왔습니다
- 음식 배달 왔습니다
- 우편물 왔습니다
- 문 앞에 두겠습니다
- 관리사무소입니다
- 경비실입니다
- 소방 점검 나왔습니다
- 도시가스 점검 나왔습니다
- 인터넷 설치 기사입니다
- 에어컨 수리 기사입니다
- 서비스센터에서 왔습니다
- 아래층 누수 확인이 필요합니다
- 긴급히 확인할 사항이 있습니다

카테고리:
- DELIVERY: 택배, 배달, 음식 배달, 우편, 물품 전달
- VISITOR: 지인, 가족, 손님, 방문자, 설치 기사, 수리 기사, 서비스센터
- NOTICE: 관리사무소, 경비실, 점검, 공지, 안내
- EMERGENCY: 화재, 가스 냄새, 응급, 위험, 긴급, 누수
- ETC: 위 항목에 명확히 해당하지 않는 경우

출력 규칙:
- 반드시 JSON만 반환한다.
- 설명 문장, 마크다운, 코드블록은 쓰지 않는다.
- finalText는 자연스럽게 다듬은 최종 문장이다.
- summary는 20자 이내 요약이다.
- category는 DELIVERY, VISITOR, NOTICE, EMERGENCY, ETC 중 하나다.
- confidence는 0.0부터 1.0 사이 숫자다.

예시:
입력: 택배 왔습니다
출력:
{{
  "finalText": "택배 왔습니다.",
  "summary": "택배 도착",
  "category": "DELIVERY",
  "confidence": 0.95
}}

입력: 자. 자. 자. 자. 택배 왔습니다
출력:
{{
  "finalText": "택배 왔습니다.",
  "summary": "택배 도착",
  "category": "DELIVERY",
  "confidence": 0.9
}}

입력: 배달 왔습니다
출력:
{{
  "finalText": "배달 왔습니다.",
  "summary": "배달 도착",
  "category": "DELIVERY",
  "confidence": 0.95
}}

입력: 관리사무소인데 점검 나왔습니다
출력:
{{
  "finalText": "관리사무소입니다. 점검 때문에 방문했습니다.",
  "summary": "점검 방문",
  "category": "NOTICE",
  "confidence": 0.9
}}

입력: 어켄 수왔습니다
출력:
{{
  "finalText": "에어컨 수리 왔습니다.",
  "summary": "에어컨 수리",
  "category": "VISITOR",
  "confidence": 0.75
}}

입력: 도시 까스 정검 나왔습니다
출력:
{{
  "finalText": "도시가스 점검 나왔습니다.",
  "summary": "가스 점검",
  "category": "NOTICE",
  "confidence": 0.85
}}

입력: 아랫층 누 수 때문에 왔습니다
출력:
{{
  "finalText": "아래층 누수 때문에 왔습니다.",
  "summary": "누수 확인",
  "category": "EMERGENCY",
  "confidence": 0.9
}}

입력:
{corrected_text}
"""

    try:
        response = client.responses.create(
            model="gpt-4.1-mini",
            input=prompt,
            temperature=0.1
        )

        result_text = response.output_text

        print("Raw STT text:", raw_text)
        print("Dictionary corrected text:", corrected_text)
        print("GPT Response:")
        print(result_text)

        result = extract_json(result_text)

        final_text = result.get("finalText", corrected_text)
        summary = result.get("summary") or make_simple_summary(final_text)
        category = result.get("category") or guess_category(final_text)
        confidence = float(result.get("confidence", 0.8))

        final_text = remove_stutter_noise(final_text)

        if not final_text:
            final_text = "내용 없음"
            summary = "내용 없음"
            category = "ETC"
            confidence = 0.0

        return RefineResponse(
            finalText=final_text,
            summary=summary,
            category=category,
            confidence=confidence
        )

    except Exception as e:
        print("GPT refine error:", str(e))

        fallback_text = remove_stutter_noise(corrected_text)

        if not fallback_text:
            return RefineResponse(
                finalText="내용 없음",
                summary="내용 없음",
                category="ETC",
                confidence=0.0
            )

        return RefineResponse(
            finalText=fallback_text,
            summary=make_simple_summary(fallback_text),
            category=guess_category(fallback_text),
            confidence=0.6
        )


# ======================
# Summary 관련
# ======================

class SummaryRequest(BaseModel):
    text: str


class SummaryResponse(BaseModel):
    summary: str


@app.post("/summarize", response_model=SummaryResponse)
def summarize(req: SummaryRequest):
    text = " ".join(req.text.strip().split())
    text = remove_stutter_noise(text)

    if not text:
        return SummaryResponse(
            summary="내용 없음"
        )

    corrected_text = apply_correction_dictionary(text)
    corrected_text = remove_stutter_noise(corrected_text)

    if not corrected_text:
        return SummaryResponse(
            summary="내용 없음"
        )

    return SummaryResponse(
        summary=make_simple_summary(corrected_text)
    )