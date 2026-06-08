from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from typing import Optional
import tempfile
import os
import json

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

        text = "".join(
            [seg.text for seg in segment_list]
        ).strip()

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

    text = " ".join(req.rawText.strip().split())

    if not text:
        return RefineResponse(
            finalText="내용 없음",
            summary="내용 없음",
            category="ETC",
            confidence=0.0
        )

    if req.maxLength and len(text) > req.maxLength:
        text = text[:req.maxLength] + "..."

    prompt = f"""
너는 아파트 인터폰 음성 인식(STT) 결과를 정리하는 AI다.

입력되는 문장은 인터폰 방문자, 택배 기사, 배달 기사, 관리사무소, 경비실, 이웃 등의 발화를 STT로 변환한 결과다.
STT 결과에는 오타, 띄어쓰기 오류, 잘못 인식된 단어, 의미가 어색한 문장이 포함될 수 있다.

너의 역할:
1. STT 결과를 자연스럽고 정중한 한국어 문장으로 정리한다.
2. 인터폰 상황에 맞게 문맥상 명백한 오인식은 보정한다.
3. 단, 원문에 없는 새로운 정보는 절대 추가하지 않는다.
4. 의미가 불확실한 내용은 과하게 추측하지 않는다.
5. 방문 목적을 카테고리로 분류한다.
6. 사용자가 앱에서 바로 볼 수 있도록 짧고 명확하게 정리한다.

카테고리 기준:
- DELIVERY: 택배, 배달, 음식 배달, 우편, 물품 전달
- VISITOR: 지인, 가족, 손님, 방문자
- NOTICE: 관리사무소, 경비실, 점검, 공지, 안내
- EMERGENCY: 화재, 가스, 응급, 위험, 긴급 상황
- ETC: 위 항목에 명확히 해당하지 않는 경우

출력 규칙:
- 반드시 JSON만 반환한다.
- 설명 문장, 마크다운, 코드블록은 절대 쓰지 않는다.
- finalText는 정중한 문장으로 작성한다.
- summary는 20자 이내의 한 줄 요약으로 작성한다.
- category는 DELIVERY, VISITOR, NOTICE, EMERGENCY, ETC 중 하나만 사용한다.
- confidence는 0.0부터 1.0 사이 숫자로 작성한다.

예시 1:
원문: 택배 왔는데 문 앞에 둘게요
출력:
{{
  "finalText": "택배가 도착했습니다. 문 앞에 두겠습니다.",
  "summary": "택배 도착",
  "category": "DELIVERY",
  "confidence": 0.95
}}

예시 2:
원문: 관리사무소인데 소방 점검 때문에 왔습니다
출력:
{{
  "finalText": "관리사무소입니다. 소방 점검 때문에 방문했습니다.",
  "summary": "소방 점검 방문",
  "category": "NOTICE",
  "confidence": 0.95
}}

예시 3:
원문: 누구세요 친구인데요
출력:
{{
  "finalText": "친구분이 방문했습니다.",
  "summary": "방문자 도착",
  "category": "VISITOR",
  "confidence": 0.8
}}

원문:
{text}
"""

    try:
        response = client.responses.create(
            model="gpt-4.1-mini",
            input=prompt
        )

        result_text = response.output_text

        print("GPT Response:")
        print(result_text)

        result = json.loads(result_text)

        return RefineResponse(
            finalText=result.get("finalText", text),
            summary=result.get("summary", ""),
            category=result.get("category", "ETC"),
            confidence=float(result.get("confidence", 0.9))
        )

    except Exception as e:
        print("GPT refine error:", str(e))

        return RefineResponse(
            finalText=text,
            summary=text[:20] + ("..." if len(text) > 20 else ""),
            category="ETC",
            confidence=0.5
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

    if len(text) > 30:
        summary = text[:30] + "..."
    else:
        summary = text

    return SummaryResponse(
        summary=summary
    )