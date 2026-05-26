from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel
from typing import Optional
import tempfile
import os

from faster_whisper import WhisperModel

app = FastAPI()

# Whisper 모델 로드
model = WhisperModel("base", device="cpu", compute_type="int8")


# ======================
# STT 관련
# ======================

class SttResponse(BaseModel):
    rawText: str
    language: Optional[str] = None


@app.post("/stt", response_model=SttResponse)
async def stt(file: UploadFile = File(...)):
    suffix = os.path.splitext(file.filename)[1] if file.filename else ".wav"

    tmp = tempfile.NamedTemporaryFile(delete=False, suffix=suffix)
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
        print("language:", getattr(info, "language", None))

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
    maxLength: Optional[int] = 300


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

    if req.maxLength and len(text) > req.maxLength:
        text = text[: req.maxLength] + "..."

    final = f"📢 안내드립니다. {text}"

    return RefineResponse(
        finalText=final,
        summary=text[:30] + ("..." if len(text) > 30 else ""),
        category="안내",
        confidence=0.9,
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
        return SummaryResponse(summary="내용 없음")

    # 일단 테스트용 요약
    if len(text) > 30:
        summary = text[:30] + "..."
    else:
        summary = text

    return SummaryResponse(summary=summary)