CREATE TABLE "디자인프로필" (
                          "design_profile_key"	VARCHAR(255)		NOT NULL,
                          "user_id"	varchar(50)		NOT NULL,
                          "Field"	VARCHAR(255)		NULL
);

CREATE TABLE "통계" (
                      "statistics_key"	VARCHAR(255)		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL,
                      "Field3"	VARCHAR(255)		NULL,
                      "Field4"	VARCHAR(255)		NULL,
                      "Field5"	VARCHAR(255)		NULL,
                      "Field6"	VARCHAR(255)		NULL,
                      "Field7"	VARCHAR(255)		NULL
);

CREATE TABLE "팝업이미지" (
                         "popup_files_key"	VARCHAR(255)		NOT NULL,
                         "popup_key"	VARCHAR(255)		NOT NULL,
                         "files_key"	BIGINT		NOT NULL
);

CREATE TABLE "업체프로필" (
                         "company_key"	VARCHAR(255)		NOT NULL,
                         "user_id"	varchar(50)		NOT NULL,
                         "Field"	VARCHAR(255)		NULL,
                         "Field2"	VARCHAR(255)		NULL,
                         "Field3"	VARCHAR(255)		NULL,
                         "Field4"	VARCHAR(255)		NULL,
                         "Field5"	VARCHAR(255)		NULL,
                         "Field6"	VARCHAR(255)		NULL,
                         "Field7"	VARCHAR(255)		NULL,
                         "Field8"	VARCHAR(255)		NULL,
                         "Field9"	VARCHAR(255)		NULL,
                         "Field10"	VARCHAR(255)		NULL
);

CREATE TABLE "결제 이력 및 상태 관리" (
                                 "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "ai_예상견적" (
                           "ai_estimate_key"	VARCHAR(255)		NOT NULL,
                           "user_id"	varchar(50)		NOT NULL,
                           "Field2"	VARCHAR(255)		NULL,
                           "Field"	VARCHAR(255)		NULL
);

CREATE TABLE "챗봇" (
                      "chatbot_key"	VARCHAR(255)		NOT NULL,
                      "user_id"	varchar(50)		NOT NULL,
                      "Field"	VARCHAR(255)		NULL
);

CREATE TABLE "견적서" (
                       "estimate_key"	BIGINT		NOT NULL,
                       "user_id"	varchar(50)		NOT NULL,
                       "user_profile_key"	VARCHAR(255)		NOT NULL,
                       "files_key"	VARCHAR(255)		NULL,
                       "Field"	varchar(100)		NOT NULL,
                       "Field3"	timestamp		NULL,
                       "Field4"	timestamp		NULL,
                       "Field5"	enum		NOT NULL,
                       "Field6"	int		NOT NULL,
                       "Field7"	text		NULL,
                       "Field2"	enum		NULL,
                       "Field8"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "견적서"."Field4" IS '미정 추가하기';

COMMENT ON COLUMN "견적서"."Field5" IS '부분시공,리모델링,신축';

COMMENT ON COLUMN "견적서"."Field2" IS '견적서,도면신청';

COMMENT ON COLUMN "견적서"."Field8" IS '제안,계약,협의 등';

CREATE TABLE "사용자" (
                       "user_id"	varchar(50)		NOT NULL,
                       "profile_img"	VARCHAR(255)		NULL,
                       "password"	varchar(60)		NULL,
                       "name"	VARCHAR(100)		NULL,
                       "email"	VARCHAR(100)		NOT NULL,
                       "address"	varchar(60)		NULL,
                       "detail_address"	varchar(60)		NULL,
                       "number"	varchar(20)		NULL,
                       "enabled"	boolean	DEFAULT true	NULL,
                       "account_non_expired"	boolean	DEFAULT true	NULL,
                       "account_non_locked"	boolean	DEFAULT true	NULL,
                       "created_at"	TIMESTAMP	DEFAULT CURRENT_TIMESTAMP	NULL,
                       "updated_at"	TIMESTAMP	DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP	NULL,
                       "credentials_non_expired"	BOOLEAN	DEFAULT TRUE	NULL,
                       "role"	int		NOT NULL,
                       "agree"	BOOLEAN		NOT NULL,
                       "provide"	VARCHAR(255)		NULL,
                       "Field"	VARCHAR(255)		NULL,
                       "Field2"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "사용자"."role" IS '1=admin, 2=user, 3=company , 4=designer, 5=blacklist';

CREATE TABLE "공지사항/이벤트/자주묻는질문/자료실/사진방" (
                                           "board_key"	BIGINT		NOT NULL,
                                           "user_id"	varchar(50)		NOT NULL,
                                           "order_key"	DECIMAL(20,6)	DEFAULT 1.000000	NOT NULL,
                                           "title"	varchar(100)		NOT NULL,
                                           "content"	longtext		NOT NULL,
                                           "create_date"	timestamp	DEFAULT current_timestamp	NOT NULL,
                                           "update_date"	timestamp	DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP	NULL,
                                           "type"	enum	DEFAULT 공지사항/이벤트/자주묻는질문/사진방	NOT NULL,
                                           "likes"	VARCHAR(255)		NULL
);

CREATE TABLE "퀵메뉴" (
                       "Quickmenu_key"	VARCHAR(255)		NOT NULL,
                       "user_id"	varchar(50)		NOT NULL,
                       "Field"	VARCHAR(255)		NULL,
                       "Field2"	VARCHAR(255)		NULL,
                       "Field3"	VARCHAR(255)		NULL,
                       "Field4"	VARCHAR(255)		NULL,
                       "Field5"	VARCHAR(255)		NULL,
                       "Field6"	VARCHAR(255)		NULL
);

CREATE TABLE "플래너요청" (
                         "planner_key"	BIGINT		NOT NULL,
                         "user_id"	varchar(50)		NOT NULL,
                         "user_id2"	varchar(50)		NOT NULL,
                         "files_key"	BIGINT		NULL,
                         "start_date"	timestamp		NOT NULL,
                         "end_date"	timestamp		NOT NULL,
                         "content"	text		NULL,
                         "Field"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "플래너요청"."user_id" IS '플래너아이디';

COMMENT ON COLUMN "플래너요청"."user_id2" IS '사용자아이디';

COMMENT ON COLUMN "플래너요청"."Field" IS '개원컨설팅,실측요청,용도변경,감리,부동산건물체크,기타';

CREATE TABLE "답변" (
                      "answer_key"	VARCHAR(255)		NOT NULL,
                      "chatbot_key"	VARCHAR(255)		NOT NULL,
                      "answer"	VARCHAR(255)		NULL
);

CREATE TABLE "디자인 참가작" (
                           "entries_key"	VARCHAR(255)		NOT NULL,
                           "user_id"	varchar(50)		NOT NULL,
                           "contests_key"	VARCHAR(255)		NOT NULL,
                           "files_key"	BIGINT		NOT NULL,
                           "Field"	VARCHAR(255)		NULL,
                           "Field2"	VARCHAR(255)		NULL,
                           "Field3"	VARCHAR(255)		NULL,
                           "Field4"	VARCHAR(255)		NULL,
                           "Field5"	VARCHAR(255)		NULL,
                           "Field6"	VARCHAR(255)		NULL
);

CREATE TABLE "ai답변" (
                        "ai_key"	VARCHAR(255)		NOT NULL,
                        "Field"	longtext		NULL,
                        "ai_estimate_key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "디자인 콘테스트 게시판" (
                                "contests_key"	VARCHAR(255)		NOT NULL,
                                "user_id"	varchar(50)		NOT NULL,
                                "files_key"	BIGINT		NOT NULL,
                                "Field"	VARCHAR(255)		NULL,
                                "Field2"	VARCHAR(255)		NULL,
                                "start_date"	VARCHAR(255)		NULL,
                                "end_date"	VARCHAR(255)		NULL,
                                "Field3"	enum		NULL,
                                "Field4"	VARCHAR(255)		NULL,
                                "Field5"	VARCHAR(255)		NULL,
                                "Field6"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "디자인 콘테스트 게시판"."Field3" IS '진행중,완료';

CREATE TABLE "인증" (
                      "certification_key"	VARCHAR(255)		NOT NULL,
                      "user_id"	varchar(50)		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL,
                      "Field3"	VARCHAR(255)		NULL,
                      "Field4"	VARCHAR(255)		NULL,
                      "Field5"	VARCHAR(255)		NULL,
                      "Field6"	VARCHAR(255)		NULL,
                      "Field9"	VARCHAR(255)		NULL,
                      "Field7"	VARCHAR(255)		NULL,
                      "Field8"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "인증"."Field" IS 'email.sms';

CREATE TABLE "광고" (
                      "advertisement_key"	VARCHAR(255)		NOT NULL,
                      "user_id"	varchar(50)		NOT NULL,
                      "files_key"	BIGINT		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL,
                      "Field3"	VARCHAR(255)		NULL
);

CREATE TABLE "상품(서비스) 및 플랜 변경 이력" (
                                      "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "태그내용" (
                        "tag_content_key"	VARCHAR(255)		NOT NULL,
                        "tag_title_key"	VARCHAR(255)		NOT NULL,
                        "user_id"	varchar(50)		NOT NULL,
                        "Field"	VARCHAR(255)		NULL
);

CREATE TABLE "매칭/견적/디자인 공모/도면그리기 히스토리" (
                                           "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "견적제안" (
                        "suggestions_key"	BIGINT		NOT NULL,
                        "user_id"	varchar(50)		NOT NULL,
                        "user_id2"	varchar(50)		NOT NULL,
                        "estimate_key"	BIGINT		NOT NULL,
                        "Field"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "견적제안"."user_id" IS '사용자';

COMMENT ON COLUMN "견적제안"."user_id2" IS '업체';

CREATE TABLE "게시판/게시글 변경 이력" (
                                 "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "관리자 행위 이력" (
                             "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "디자인참가파일" (
                           "entries_files_key"	VARCHAR(255)		NOT NULL,
                           "entries_key"	VARCHAR(255)		NOT NULL,
                           "files_key"	BIGINT		NOT NULL
);

CREATE TABLE "환불 및 알림 로그 테이블" (
                                  "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "구독" (
                      "SubscriptionPlan_key"	VARCHAR(255)		NOT NULL,
                      "payment_key"	VARCHAR(255)		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL,
                      "Field3"	VARCHAR(255)		NULL,
                      "Field4"	VARCHAR(255)		NULL,
                      "Field5"	VARCHAR(255)		NULL,
                      "Field6"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "구독"."Field" IS '브론즈.실버,골드';

CREATE TABLE "회원 정보 변경 이력" (
                               "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "고객후기" (
                        "Key"	VARCHAR(255)		NOT NULL,
                        "payment_key"	VARCHAR(255)		NOT NULL,
                        "files_key"	BIGINT		NULL,
                        "Field"	VARCHAR(255)		NULL,
                        "Field2"	VARCHAR(255)		NULL,
                        "Field3"	VARCHAR(255)		NULL,
                        "Field4"	VARCHAR(255)		NULL,
                        "Field5"	VARCHAR(255)		NULL
);

CREATE TABLE "사용자프로필" (
                          "user_profile_key"	VARCHAR(255)		NOT NULL,
                          "user_id"	varchar(50)		NOT NULL,
                          "files_key"	VARCHAR(255)		NULL,
                          "Field"	VARCHAR(255)		NULL,
                          "Field2"	VARCHAR(255)		NULL,
                          "Field3"	VARCHAR(255)		NULL,
                          "Field4"	VARCHAR(255)		NULL
);

CREATE TABLE "링크" (
                      "link_key"	VARCHAR(255)		NOT NULL,
                      "Quickmenu_key"	VARCHAR(255)		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL
);

CREATE TABLE "매칭시스템" (
                         "matching_key"	VARCHAR(255)		NOT NULL,
                         "user_id"	varchar(50)		NOT NULL,
                         "user_id2"	varchar(50)		NOT NULL,
                         "status"	VARCHAR(255)		NULL,
                         "Field2"	VARCHAR(255)		NULL,
                         "Field3"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "매칭시스템"."user_id" IS '업체';

COMMENT ON COLUMN "매칭시스템"."user_id2" IS '사용자';

COMMENT ON COLUMN "매칭시스템"."status" IS '진행, 확정, 취소';

CREATE TABLE "활동분석" (
                        "analysis_key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "업체파일" (
                        "company_files_key"	VARCHAR(255)		NOT NULL,
                        "company_key"	VARCHAR(255)		NOT NULL,
                        "files_key"	BIGINT		NOT NULL
);

CREATE TABLE "도면요청" (
                        "Key"	VARCHAR(255)		NOT NULL,
                        "user_id"	varchar(50)		NOT NULL,
                        "user_id2"	varchar(50)		NOT NULL,
                        "files_key"	BIGINT		NOT NULL,
                        "Field"	VARCHAR(255)		NULL,
                        "Field2"	VARCHAR(255)		NULL,
                        "Field3"	VARCHAR(255)		NULL,
                        "Field4"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "도면요청"."user_id" IS '사용자';

COMMENT ON COLUMN "도면요청"."user_id2" IS '업체';

COMMENT ON COLUMN "도면요청"."Field" IS '부분시공,리모델링,신축';

CREATE TABLE "구독 상태" (
                         "Key"	VARCHAR(255)		NOT NULL,
                         "user_id"	varchar(50)		NOT NULL,
                         "SubscriptionPlan_key"	VARCHAR(255)		NOT NULL,
                         "Field"	VARCHAR(255)		NULL,
                         "Field2"	VARCHAR(255)		NULL,
                         "Field3"	VARCHAR(255)		NULL,
                         "Field4"	VARCHAR(255)		NULL,
                         "Field5"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "구독 상태"."Field3" IS '상태 (활성, 해지, 일시중지)';

CREATE TABLE "결제" (
                      "payment_key"	VARCHAR(255)		NOT NULL,
                      "user_id"	varchar(50)		NOT NULL,
                      "estimate_key"	BIGINT		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL,
                      "Field3"	VARCHAR(255)		NULL,
                      "Field4"	VARCHAR(255)		NULL,
                      "Field5"	VARCHAR(255)		NULL,
                      "Field6"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "결제"."Field6" IS '성공,실패,취소';

CREATE TABLE "ai_api_활용하기" (
                               "Key"	VARCHAR(255)		NOT NULL
);

CREATE TABLE "이메일전송" (
                         "Key"	VARCHAR(255)		NOT NULL,
                         "files_key"	BIGINT		NOT NULL,
                         "Field"	VARCHAR(255)		NULL,
                         "Field2"	VARCHAR(255)		NULL
);

CREATE TABLE "사진 및 파일" (
                           "files_key"	BIGINT		NOT NULL,
                           "original_filename"	VARCHAR(255)		NULL,
                           "stored_filename"	VARCHAR(255)		NULL,
                           "channel"	VARCHAR(100)	DEFAULT enum	NULL,
                           "type"	VARCHAR(255)		NULL,
                           "Field"	VARCHAR(255)		NULL
);

COMMENT ON COLUMN "사진 및 파일"."channel" IS '뭐뭐할지 고민';

CREATE TABLE "팝업" (
                      "popup_key"	VARCHAR(255)		NOT NULL,
                      "user_id"	varchar(50)		NOT NULL,
                      "Field"	VARCHAR(255)		NULL,
                      "Field2"	VARCHAR(255)		NULL,
                      "Field3"	VARCHAR(255)		NULL,
                      "Field4"	VARCHAR(255)		NULL,
                      "files_key"	VARCHAR(255)		NULL
);

CREATE TABLE "게시판파일" (
                         "board_files_key"	BIGINT		NOT NULL,
                         "board_key"	BIGINT		NOT NULL,
                         "files_key"	BIGINT		NOT NULL
);

CREATE TABLE "태그_대제목" (
                          "tag_title_key"	VARCHAR(255)		NOT NULL,
                          "board_key"	BIGINT	DEFAULT autoincrement	NOT NULL,
                          "user_id"	varchar(50)		NOT NULL,
                          "Field"	VARCHAR(255)		NULL
);

ALTER TABLE "디자인프로필" ADD CONSTRAINT "PK_디자인프로필" PRIMARY KEY (
                                                             "design_profile_key"
    );

ALTER TABLE "통계" ADD CONSTRAINT "PK_통계" PRIMARY KEY (
                                                     "statistics_key"
    );

ALTER TABLE "팝업이미지" ADD CONSTRAINT "PK_팝업이미지" PRIMARY KEY (
                                                           "popup_files_key"
    );

ALTER TABLE "업체프로필" ADD CONSTRAINT "PK_업체프로필" PRIMARY KEY (
                                                           "company_key"
    );

ALTER TABLE "결제 이력 및 상태 관리" ADD CONSTRAINT "PK_결제 이력 및 상태 관리" PRIMARY KEY (
                                                                           "Key"
    );

ALTER TABLE "ai_예상견적" ADD CONSTRAINT "PK_AI_예상견적" PRIMARY KEY (
                                                               "ai_estimate_key"
    );

ALTER TABLE "챗봇" ADD CONSTRAINT "PK_챗봇" PRIMARY KEY (
                                                     "chatbot_key"
    );

ALTER TABLE "견적서" ADD CONSTRAINT "PK_견적서" PRIMARY KEY (
                                                       "estimate_key"
    );

ALTER TABLE "사용자" ADD CONSTRAINT "PK_사용자" PRIMARY KEY (
                                                       "user_id"
    );

ALTER TABLE "공지사항/이벤트/자주묻는질문/자료실/사진방" ADD CONSTRAINT "PK_공지사항/이벤트/자주묻는질문/자료실/사진방" PRIMARY KEY (
                                                                                               "board_key"
    );

ALTER TABLE "퀵메뉴" ADD CONSTRAINT "PK_퀵메뉴" PRIMARY KEY (
                                                       "Quickmenu_key"
    );

ALTER TABLE "플래너요청" ADD CONSTRAINT "PK_플래너요청" PRIMARY KEY (
                                                           "planner_key"
    );

ALTER TABLE "답변" ADD CONSTRAINT "PK_답변" PRIMARY KEY (
                                                     "answer_key"
    );

ALTER TABLE "디자인 참가작" ADD CONSTRAINT "PK_디자인 참가작" PRIMARY KEY (
                                                               "entries_key"
    );

ALTER TABLE "ai답변" ADD CONSTRAINT "PK_AI답변" PRIMARY KEY (
                                                         "ai_key"
    );

ALTER TABLE "디자인 콘테스트 게시판" ADD CONSTRAINT "PK_디자인 콘테스트 게시판" PRIMARY KEY (
                                                                         "contests_key"
    );

ALTER TABLE "인증" ADD CONSTRAINT "PK_인증" PRIMARY KEY (
                                                     "certification_key"
    );

ALTER TABLE "광고" ADD CONSTRAINT "PK_광고" PRIMARY KEY (
                                                     "advertisement_key"
    );

ALTER TABLE "상품(서비스) 및 플랜 변경 이력" ADD CONSTRAINT "PK_상품(서비스) 및 플랜 변경 이력" PRIMARY KEY (
                                                                                     "Key"
    );

ALTER TABLE "태그내용" ADD CONSTRAINT "PK_태그내용" PRIMARY KEY (
                                                         "tag_content_key"
    );

ALTER TABLE "매칭/견적/디자인 공모/도면그리기 히스토리" ADD CONSTRAINT "PK_매칭/견적/디자인 공모/도면그리기 히스토리" PRIMARY KEY (
                                                                                               "Key"
    );

ALTER TABLE "견적제안" ADD CONSTRAINT "PK_견적제안" PRIMARY KEY (
                                                         "suggestions_key"
    );

ALTER TABLE "게시판/게시글 변경 이력" ADD CONSTRAINT "PK_게시판/게시글 변경 이력" PRIMARY KEY (
                                                                           "Key"
    );

ALTER TABLE "관리자 행위 이력" ADD CONSTRAINT "PK_관리자 행위 이력" PRIMARY KEY (
                                                                   "Key"
    );

ALTER TABLE "디자인참가파일" ADD CONSTRAINT "PK_디자인참가파일" PRIMARY KEY (
                                                               "entries_files_key"
    );

ALTER TABLE "환불 및 알림 로그 테이블" ADD CONSTRAINT "PK_환불 및 알림 로그 테이블" PRIMARY KEY (
                                                                             "Key"
    );

ALTER TABLE "구독" ADD CONSTRAINT "PK_구독" PRIMARY KEY (
                                                     "SubscriptionPlan_key"
    );

ALTER TABLE "회원 정보 변경 이력" ADD CONSTRAINT "PK_회원 정보 변경 이력" PRIMARY KEY (
                                                                       "Key"
    );

ALTER TABLE "고객후기" ADD CONSTRAINT "PK_고객후기" PRIMARY KEY (
                                                         "Key"
    );

ALTER TABLE "사용자프로필" ADD CONSTRAINT "PK_사용자프로필" PRIMARY KEY (
                                                             "user_profile_key"
    );

ALTER TABLE "링크" ADD CONSTRAINT "PK_링크" PRIMARY KEY (
                                                     "link_key"
    );

ALTER TABLE "매칭시스템" ADD CONSTRAINT "PK_매칭시스템" PRIMARY KEY (
                                                           "matching_key"
    );

ALTER TABLE "활동분석" ADD CONSTRAINT "PK_활동분석" PRIMARY KEY (
                                                         "analysis_key"
    );

ALTER TABLE "업체파일" ADD CONSTRAINT "PK_업체파일" PRIMARY KEY (
                                                         "company_files_key"
    );

ALTER TABLE "도면요청" ADD CONSTRAINT "PK_도면요청" PRIMARY KEY (
                                                         "Key"
    );

ALTER TABLE "구독 상태" ADD CONSTRAINT "PK_구독 상태" PRIMARY KEY (
                                                           "Key"
    );

ALTER TABLE "결제" ADD CONSTRAINT "PK_결제" PRIMARY KEY (
                                                     "payment_key"
    );

ALTER TABLE "ai_api_활용하기" ADD CONSTRAINT "PK_AI_API_활용하기" PRIMARY KEY (
                                                                       "Key"
    );

ALTER TABLE "이메일전송" ADD CONSTRAINT "PK_이메일전송" PRIMARY KEY (
                                                           "Key"
    );

ALTER TABLE "사진 및 파일" ADD CONSTRAINT "PK_사진 및 파일" PRIMARY KEY (
                                                               "files_key"
    );

ALTER TABLE "팝업" ADD CONSTRAINT "PK_팝업" PRIMARY KEY (
                                                     "popup_key"
    );

ALTER TABLE "게시판파일" ADD CONSTRAINT "PK_게시판파일" PRIMARY KEY (
                                                           "board_files_key"
    );

ALTER TABLE "태그_대제목" ADD CONSTRAINT "PK_태그_대제목" PRIMARY KEY (
                                                             "tag_title_key"
    );

ALTER TABLE "디자인프로필" ADD CONSTRAINT "FK_사용자_TO_디자인프로필_1" FOREIGN KEY (
                                                                      "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "팝업이미지" ADD CONSTRAINT "FK_팝업_TO_팝업이미지_1" FOREIGN KEY (
                                                                   "popup_key"
    )
    REFERENCES "팝업" (
                     "popup_key"
        );

ALTER TABLE "팝업이미지" ADD CONSTRAINT "FK_사진 및 파일_TO_팝업이미지_1" FOREIGN KEY (
                                                                        "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "업체프로필" ADD CONSTRAINT "FK_사용자_TO_업체프로필_1" FOREIGN KEY (
                                                                    "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "ai_예상견적" ADD CONSTRAINT "FK_사용자_TO_ai_예상견적_1" FOREIGN KEY (
                                                                        "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "챗봇" ADD CONSTRAINT "FK_사용자_TO_챗봇_1" FOREIGN KEY (
                                                              "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "견적서" ADD CONSTRAINT "FK_사용자_TO_견적서_1" FOREIGN KEY (
                                                                "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "견적서" ADD CONSTRAINT "FK_사용자프로필_TO_견적서_1" FOREIGN KEY (
                                                                   "user_profile_key"
    )
    REFERENCES "사용자프로필" (
                         "user_profile_key"
        );

ALTER TABLE "공지사항/이벤트/자주묻는질문/자료실/사진방" ADD CONSTRAINT "FK_사용자_TO_공지사항/이벤트/자주묻는질문/자료실/사진방_1" FOREIGN KEY (
                                                                                                        "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "퀵메뉴" ADD CONSTRAINT "FK_사용자_TO_퀵메뉴_1" FOREIGN KEY (
                                                                "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "플래너요청" ADD CONSTRAINT "FK_사용자_TO_플래너요청_1" FOREIGN KEY (
                                                                    "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "플래너요청" ADD CONSTRAINT "FK_사용자_TO_플래너요청_2" FOREIGN KEY (
                                                                    "user_id2"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "답변" ADD CONSTRAINT "FK_챗봇_TO_답변_1" FOREIGN KEY (
                                                             "chatbot_key"
    )
    REFERENCES "챗봇" (
                     "chatbot_key"
        );

ALTER TABLE "디자인 참가작" ADD CONSTRAINT "FK_사용자_TO_디자인 참가작_1" FOREIGN KEY (
                                                                        "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "디자인 참가작" ADD CONSTRAINT "FK_디자인 콘테스트 게시판_TO_디자인 참가작_1" FOREIGN KEY (
                                                                                 "contests_key"
    )
    REFERENCES "디자인 콘테스트 게시판" (
                               "contests_key"
        );

ALTER TABLE "디자인 참가작" ADD CONSTRAINT "FK_사진 및 파일_TO_디자인 참가작_1" FOREIGN KEY (
                                                                            "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "ai답변" ADD CONSTRAINT "FK_ai_예상견적_TO_ai답변_1" FOREIGN KEY (
                                                                      "ai_estimate_key"
    )
    REFERENCES "ai_예상견적" (
                          "ai_estimate_key"
        );

ALTER TABLE "디자인 콘테스트 게시판" ADD CONSTRAINT "FK_사용자_TO_디자인 콘테스트 게시판_1" FOREIGN KEY (
                                                                                  "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "디자인 콘테스트 게시판" ADD CONSTRAINT "FK_사진 및 파일_TO_디자인 콘테스트 게시판_1" FOREIGN KEY (
                                                                                      "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "인증" ADD CONSTRAINT "FK_사용자_TO_인증_1" FOREIGN KEY (
                                                              "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "광고" ADD CONSTRAINT "FK_사용자_TO_광고_1" FOREIGN KEY (
                                                              "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "광고" ADD CONSTRAINT "FK_사진 및 파일_TO_광고_1" FOREIGN KEY (
                                                                  "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "태그내용" ADD CONSTRAINT "FK_태그_대제목_TO_태그내용_1" FOREIGN KEY (
                                                                     "tag_title_key"
    )
    REFERENCES "태그_대제목" (
                         "tag_title_key"
        );

ALTER TABLE "태그내용" ADD CONSTRAINT "FK_사용자_TO_태그내용_1" FOREIGN KEY (
                                                                  "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "견적제안" ADD CONSTRAINT "FK_사용자_TO_견적제안_1" FOREIGN KEY (
                                                                  "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "견적제안" ADD CONSTRAINT "FK_사용자_TO_견적제안_2" FOREIGN KEY (
                                                                  "user_id2"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "견적제안" ADD CONSTRAINT "FK_견적서_TO_견적제안_1" FOREIGN KEY (
                                                                  "estimate_key"
    )
    REFERENCES "견적서" (
                      "estimate_key"
        );

ALTER TABLE "디자인참가파일" ADD CONSTRAINT "FK_디자인 참가작_TO_디자인참가파일_1" FOREIGN KEY (
                                                                            "entries_key"
    )
    REFERENCES "디자인 참가작" (
                          "entries_key"
        );

ALTER TABLE "디자인참가파일" ADD CONSTRAINT "FK_사진 및 파일_TO_디자인참가파일_1" FOREIGN KEY (
                                                                            "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "구독" ADD CONSTRAINT "FK_결제_TO_구독_1" FOREIGN KEY (
                                                             "payment_key"
    )
    REFERENCES "결제" (
                     "payment_key"
        );

ALTER TABLE "고객후기" ADD CONSTRAINT "FK_결제_TO_고객후기_1" FOREIGN KEY (
                                                                 "payment_key"
    )
    REFERENCES "결제" (
                     "payment_key"
        );

ALTER TABLE "고객후기" ADD CONSTRAINT "FK_사진 및 파일_TO_고객후기_1" FOREIGN KEY (
                                                                      "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "사용자프로필" ADD CONSTRAINT "FK_사용자_TO_사용자프로필_1" FOREIGN KEY (
                                                                      "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "링크" ADD CONSTRAINT "FK_퀵메뉴_TO_링크_1" FOREIGN KEY (
                                                              "Quickmenu_key"
    )
    REFERENCES "퀵메뉴" (
                      "Quickmenu_key"
        );

ALTER TABLE "매칭시스템" ADD CONSTRAINT "FK_사용자_TO_매칭시스템_1" FOREIGN KEY (
                                                                    "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "매칭시스템" ADD CONSTRAINT "FK_사용자_TO_매칭시스템_2" FOREIGN KEY (
                                                                    "user_id2"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "업체파일" ADD CONSTRAINT "FK_업체프로필_TO_업체파일_1" FOREIGN KEY (
                                                                    "company_key"
    )
    REFERENCES "업체프로필" (
                        "company_key"
        );

ALTER TABLE "업체파일" ADD CONSTRAINT "FK_사진 및 파일_TO_업체파일_1" FOREIGN KEY (
                                                                      "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "도면요청" ADD CONSTRAINT "FK_사용자_TO_도면요청_1" FOREIGN KEY (
                                                                  "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "도면요청" ADD CONSTRAINT "FK_사용자_TO_도면요청_2" FOREIGN KEY (
                                                                  "user_id2"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "도면요청" ADD CONSTRAINT "FK_사진 및 파일_TO_도면요청_1" FOREIGN KEY (
                                                                      "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "구독 상태" ADD CONSTRAINT "FK_사용자_TO_구독 상태_1" FOREIGN KEY (
                                                                    "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "구독 상태" ADD CONSTRAINT "FK_구독_TO_구독 상태_1" FOREIGN KEY (
                                                                   "SubscriptionPlan_key"
    )
    REFERENCES "구독" (
                     "SubscriptionPlan_key"
        );

ALTER TABLE "결제" ADD CONSTRAINT "FK_사용자_TO_결제_1" FOREIGN KEY (
                                                              "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "결제" ADD CONSTRAINT "FK_견적서_TO_결제_1" FOREIGN KEY (
                                                              "estimate_key"
    )
    REFERENCES "견적서" (
                      "estimate_key"
        );

ALTER TABLE "이메일전송" ADD CONSTRAINT "FK_사진 및 파일_TO_이메일전송_1" FOREIGN KEY (
                                                                        "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "팝업" ADD CONSTRAINT "FK_사용자_TO_팝업_1" FOREIGN KEY (
                                                              "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

ALTER TABLE "게시판파일" ADD CONSTRAINT "FK_공지사항/이벤트/자주묻는질문/자료실/사진방_TO_게시판파일_1" FOREIGN KEY (
                                                                                        "board_key"
    )
    REFERENCES "공지사항/이벤트/자주묻는질문/자료실/사진방" (
                                          "board_key"
        );

ALTER TABLE "게시판파일" ADD CONSTRAINT "FK_사진 및 파일_TO_게시판파일_1" FOREIGN KEY (
                                                                        "files_key"
    )
    REFERENCES "사진 및 파일" (
                          "files_key"
        );

ALTER TABLE "태그_대제목" ADD CONSTRAINT "FK_공지사항/이벤트/자주묻는질문/자료실/사진방_TO_태그_대제목_1" FOREIGN KEY (
                                                                                          "board_key"
    )
    REFERENCES "공지사항/이벤트/자주묻는질문/자료실/사진방" (
                                          "board_key"
        );

ALTER TABLE "태그_대제목" ADD CONSTRAINT "FK_사용자_TO_태그_대제목_1" FOREIGN KEY (
                                                                      "user_id"
    )
    REFERENCES "사용자" (
                      "user_id"
        );

