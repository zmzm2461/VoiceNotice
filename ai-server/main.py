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
    너는 아파트 인터폰 STT 결과를 후처리하는 AI다.

    입력 문장은 인터폰 상황에서 방문자가 말한 음성을 STT로 변환한 결과다.
    STT에는 비슷한 발음으로 잘못 인식된 단어가 포함될 수 있다.

    너의 역할:
    1. STT 원문을 자연스럽고 정중한 한국어로 다듬는다.
    2. 인터폰 상황에서 자주 나오는 표현으로 보이는 경우, 비슷한 발음의 오인식을 보정한다.
    3. 단, 의미가 불확실하면 원문을 크게 바꾸지 않는다.
    4. 원문에 없는 새로운 정보는 추가하지 않는다.

    자주 나오는 인터폰 표현:
    - 택배 왔습니다
    - 배달 왔습니다
    - 음식 배달입니다
    - 문 앞에 두겠습니다
    - 문 열어주세요
    - 관리사무소입니다
    - 경비실입니다
    - 점검 나왔습니다
    - 소방 점검입니다
    - 가스 점검입니다
    - 방문했습니다
    - 부재중이신가요

    오인식 보정 기준:
    - 발음이 비슷하고, 인터폰 상황에서 훨씬 자연스러운 표현이면 보정한다.
    - 예: "특별 왔습니다" → "택배 왔습니다"
    - 예: "배달 와 씁니다" → "배달 왔습니다"
    - 예: "문 앞에 둘게여" → "문 앞에 두겠습니다"
    - 예: "관리 소입니다" → "관리사무소입니다"
    - 예: "소방 정검" → "소방 점검"
    - 단, 확실하지 않으면 억지로 바꾸지 않는다.

    출력 규칙:
    - 반드시 JSON만 반환한다.
    - finalText는 보정된 최종 문장이다.
    - summary는 20자 이내 요약이다.
    - category는 DELIVERY, VISITOR, NOTICE, EMERGENCY, ETC 중 하나다.
    - confidence는 보정이 확실하면 0.85 이상, 애매하면 0.6 이하로 한다.

    예시 1:
    원문: 특별 왔습니다
    출력:
    {{
      "finalText": "택배 왔습니다.",
      "summary": "택배 도착",
      "category": "DELIVERY",
      "confidence": 0.9
    }}

    예시 2:
    원문: 배달 와 씁니다
    출력:
    {{
      "finalText": "배달 왔습니다.",
      "summary": "배달 도착",
      "category": "DELIVERY",
      "confidence": 0.9
    }}

    예시 3:
    원문: 관리 소인데 점검 나왔습니다
    출력:
    {{
      "finalText": "관리사무소인데 점검 나왔습니다.",
      "summary": "점검 방문",
      "category": "NOTICE",
      "confidence": 0.85
    }}

    예시 4:
    원문: 뭐라고 했는지 잘 모르겠습니다
    출력:
    {{
      "finalText": "뭐라고 했는지 잘 모르겠습니다.",
      "summary": "내용 불명확",
      "category": "ETC",
      "confidence": 0.5
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