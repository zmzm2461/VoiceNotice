from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from typing import Optional
import tempfile
import os
import json
import re

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
# 교정 사전
# ======================

def apply_correction_dictionary(text: str) -> str:
    """
    STT가 자주 틀리는 인터폰 표현을 1차로 보정한다.
    GPT가 과하게 추측하기 전에 확실한 오인식만 먼저 바꾼다.
    """

    correction_map = {
        "특별 왔습니다": "택배 왔습니다",
        "특별왔습니다": "택배 왔습니다",
        "특별 왔어요": "택배 왔어요",
        "특별왔어요": "택배 왔어요",

        "달 왔습니다": "배달 왔습니다",
        "달왔습니다": "배달 왔습니다",
        "달 왔어요": "배달 왔어요",
        "달왔어요": "배달 왔어요",

        "배달 와 씁니다": "배달 왔습니다",
        "배달왔 씁니다": "배달 왔습니다",
        "배달 왔 씁니다": "배달 왔습니다",

        "관리 소입니다": "관리사무소입니다",
        "관리소입니다": "관리사무소입니다",
        "관리 소": "관리사무소",

        "경비 실입니다": "경비실입니다",
        "경비 실": "경비실",

        "정검": "점검",
        "소방 정검": "소방 점검",
        "가스 정검": "가스 점검",

        "문압": "문 앞",
        "문 압": "문 앞",
        "문앞에 둘게여": "문 앞에 둘게요",
        "문 앞에 둘게여": "문 앞에 둘게요",
        "문 앞에 두고 갈께요": "문 앞에 두고 갈게요",
    }

    corrected = text

    for wrong, right in correction_map.items():
        corrected = corrected.replace(wrong, right)

    return corrected


def guess_category(text: str) -> str:
    """
    GPT 실패 시 사용할 간단한 카테고리 분류.
    """
    if any(word in text for word in ["택배", "배달", "음식", "우편", "소포"]):
        return "DELIVERY"

    if any(word in text for word in ["관리사무소", "관리실", "경비실", "점검", "소방", "가스"]):
        return "NOTICE"

    if any(word in text for word in ["불", "화재", "응급", "위험", "긴급", "가스 냄새"]):
        return "EMERGENCY"

    if any(word in text for word in ["친구", "가족", "손님", "방문"]):
        return "VISITOR"

    return "ETC"


def make_simple_summary(text: str) -> str:
    category = guess_category(text)

    if category == "DELIVERY":
        if "택배" in text:
            return "택배 도착"
        if "배달" in text:
            return "배달 도착"
        return "배송 도착"

    if category == "NOTICE":
        if "점검" in text:
            return "점검 방문"
        return "관리 안내"

    if category == "EMERGENCY":
        return "긴급 상황"

    if category == "VISITOR":
        return "방문자 도착"

    return text[:20] + ("..." if len(text) > 20 else "")


def extract_json(text: str) -> dict:
    """
    GPT가 혹시 JSON 앞뒤에 불필요한 문장을 붙였을 때 대비.
    """
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", text, re.DOTALL)
        if match:
            return json.loads(match.group())
        raise


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
    style: Optional[str] = "apartment_notice"
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

    if not raw_text:
        return RefineResponse(
            finalText="내용 없음",
            summary="내용 없음",
            category="ETC",
            confidence=0.0
        )

    corrected_text = apply_correction_dictionary(raw_text)

    if req.maxLength and len(corrected_text) > req.maxLength:
        corrected_text = corrected_text[:req.maxLength] + "..."

    prompt = f"""
너는 아파트 인터폰 STT 문장을 자연스럽게 다듬는 후처리 AI다.

이미 1차 오인식 교정은 완료되었다.
너는 아래 문장을 과하게 바꾸지 말고, 사용자가 읽기 좋게만 정리한다.

중요 규칙:
1. 원문에 없는 내용을 추가하지 않는다.
2. 의미를 추측해서 새로 만들지 않는다.
3. 말투, 띄어쓰기, 조사, 어미만 자연스럽게 다듬는다.
4. 문장이 이미 자연스러우면 거의 그대로 둔다.
5. 확실하지 않으면 억지로 바꾸지 않는다.
6. 광고문이나 안내방송처럼 과하게 꾸미지 않는다.
7. 짧은 문장은 짧게 유지한다.

카테고리:
- DELIVERY: 택배, 배달, 음식 배달, 우편, 물품 전달
- VISITOR: 지인, 가족, 손님, 방문자
- NOTICE: 관리사무소, 경비실, 점검, 공지, 안내
- EMERGENCY: 화재, 가스, 응급, 위험, 긴급 상황
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

        return RefineResponse(
            finalText=final_text,
            summary=summary,
            category=category,
            confidence=confidence
        )

    except Exception as e:
        print("GPT refine error:", str(e))

        return RefineResponse(
            finalText=corrected_text,
            summary=make_simple_summary(corrected_text),
            category=guess_category(corrected_text),
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

    if not text:
        return SummaryResponse(
            summary="내용 없음"
        )

    corrected_text = apply_correction_dictionary(text)

    return SummaryResponse(
        summary=make_simple_summary(corrected_text)
    )