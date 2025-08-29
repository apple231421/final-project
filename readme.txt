# 🏥 Final Project - Hospital Web Application

> **Spring Boot + Thymeleaf 기반 병원 웹 애플리케이션**  
> 진료과/의사 관리, 예약, 게시판(공지/뉴스/보도), Q&A, 자원봉사,  
> OAuth 로그인, 이메일 발송, 챗봇(OpenAI) 등 **종합 기능**을 갖춘 프로젝트입니다.  
---

## ⚙️ 기술 스택

- **Backend**: Spring Boot (Security, Web, JPA)  
- **Frontend**: Thymeleaf (HTML 템플릿)  
- **Database**: MySQL  
- **Authentication**: OAuth2 (Google, Kakao)  
- **Email**: Gmail SMTP  
- **AI 연동**: OpenAI API  
- **빌드 도구**: Gradle Wrapper  
- **Java 버전**: 17+ (Java 21 권장 가능)

---

📝 주요 기능

👥 회원/로그인
Spring Security + OAuth2 (Google, Kakao 로그인 지원)

📅 예약 시스템
의사/스케줄 관리

예약 생성/조회/취소

예약 가능 시간 확인

📢 게시판
공지사항, 뉴스, 보도자료 CRUD

휴지통 관리

❓ Q&A
문의 등록

답변 관리

상세 조회

🙋 자원봉사
신청 / 내 활동 확인 / 상태 관리

🤖 챗봇/AI
OpenAI API 연동 (ChatController)

📧 이메일 발송
Gmail SMTP 기반 인증/알림 메일 전송

📄 PDF 출력
진단서 / 문서 PDF 생성
