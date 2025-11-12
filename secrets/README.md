# Gmail Service Account 키 파일 설정

이 디렉토리에 Google Service Account 키 파일을 저장합니다.

## 파일 위치
- `service-account.json` - Gmail API Service Account 키 파일

## 주의사항
⚠️ **절대 Git에 커밋하지 마세요!**
- 이 디렉토리는 `.gitignore`에 등록되어 있습니다.
- 키 파일이 유출되면 보안 위험이 있습니다.

## 설정 방법
1. Google Cloud Console에서 Service Account 키 파일 다운로드
2. `secrets/service-account.json`으로 저장
3. `.env` 파일에 환경변수 설정:
   ```
   GMAIL_API_ENABLED=true
   GMAIL_DELEGATED_USER=noreply@yourdomain.com
   GMAIL_SERVICE_ACCOUNT_KEY=file:./secrets/service-account.json
   GMAIL_APP_NAME=damoa
   ```
