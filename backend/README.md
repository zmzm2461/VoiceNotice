# VoiceNotice Backend (Refactored)

This backend was refactored from the apartment notice domain to an intercom captioning domain.

## Core flow
1. Register device
2. Start session
3. Upload audio chunks
4. Run STT per chunk
5. Read partial transcripts
6. Finalize session and refine merged text

## Main endpoints
- `POST /api/devices/register`
- `POST /api/sessions/start`
- `POST /api/audio/chunk`
- `GET /api/sessions/{sessionId}/transcripts`
- `POST /api/sessions/{sessionId}/finalize`
- `GET /api/sessions/{sessionId}/final`
