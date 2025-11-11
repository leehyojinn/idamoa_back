-- ===========================================
-- V2: 이메일 인증 템플릿 추가
-- ===========================================

-- 1. notification_templates 테이블에 BaseEntity 필드 추가
ALTER TABLE public.notification_templates
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT false NOT NULL,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS metadata JSONB DEFAULT '{}'::jsonb;

-- 2. 이메일 인증 코드 템플릿 (회원가입용)
INSERT INTO public.notification_templates (
    id,
    uuid,
    name,
    code,
    channel,
    title_template,
    content_template,
    variables,
    is_active,
    description,
    is_deleted,
    deleted_at,
    metadata,
    created_at,
    updated_at
) VALUES (
    nextval('notification_templates_id_seq'),
    gen_random_uuid(),
    '이메일 인증 코드 (회원가입)',
    'EMAIL_VERIFICATION_SIGNUP',
    'EMAIL',
    '[다모아] 이메일 인증 코드',
    '<div style="font-family: ''Apple SD Gothic Neo'', ''Malgun Gothic'', ''맑은 고딕'', system-ui, -apple-system, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px;">
    <div style="text-align: center; margin-bottom: 40px;">
        <h1 style="color: #333; font-size: 28px; font-weight: 700; margin: 0;">다모아</h1>
        <p style="color: #666; font-size: 14px; margin-top: 8px;">인테리어 전문가 매칭 플랫폼</p>
    </div>

    <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); border-radius: 12px; padding: 30px; color: white; text-align: center; margin-bottom: 30px;">
        <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600;">이메일 인증 코드</h2>
        <div style="background: rgba(255,255,255,0.95); border-radius: 8px; padding: 20px; margin: 20px 0;">
            <p style="margin: 0; color: #333; font-size: 14px; margin-bottom: 12px;">아래 인증 코드를 입력해주세요</p>
            <div style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #667eea; font-family: Consolas, Monaco, monospace;">
                {{verificationCode}}
            </div>
        </div>
        <p style="margin: 16px 0 0 0; font-size: 14px; opacity: 0.9;">유효시간: {{expiryMinutes}}분</p>
    </div>

    <div style="background: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
        <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #333;">💡 안내사항</h3>
        <ul style="margin: 0; padding-left: 20px; color: #666; font-size: 14px; line-height: 1.8;">
            <li>본인이 요청하지 않은 경우, 이 이메일을 무시하셔도 됩니다.</li>
            <li>인증 코드는 {{expiryMinutes}}분 후 자동으로 만료됩니다.</li>
            <li>인증 코드는 다른 사람과 공유하지 마세요.</li>
        </ul>
    </div>

    <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e0e0e0; color: #999; font-size: 12px;">
        <p style="margin: 0 0 8px 0;">본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.</p>
        <p style="margin: 0;">© 2025 다모아(Damoa). All rights reserved.</p>
    </div>
</div>',
    '{"verificationCode": "6자리 숫자 인증 코드", "expiryMinutes": "만료 시간(분)"}'::jsonb,
    true,
    '회원가입 시 이메일 인증 코드 발송용 템플릿',
    false,
    NULL,
    '{}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 3. 이메일 인증 코드 템플릿 (비밀번호 재설정용)
INSERT INTO public.notification_templates (
    id,
    uuid,
    name,
    code,
    channel,
    title_template,
    content_template,
    variables,
    is_active,
    description,
    is_deleted,
    deleted_at,
    metadata,
    created_at,
    updated_at
) VALUES (
    nextval('notification_templates_id_seq'),
    gen_random_uuid(),
    '이메일 인증 코드 (비밀번호 재설정)',
    'EMAIL_VERIFICATION_PASSWORD_RESET',
    'EMAIL',
    '[다모아] 비밀번호 재설정 인증 코드',
    '<div style="font-family: ''Apple SD Gothic Neo'', ''Malgun Gothic'', ''맑은 고딕'', system-ui, -apple-system, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px;">
    <div style="text-align: center; margin-bottom: 40px;">
        <h1 style="color: #333; font-size: 28px; font-weight: 700; margin: 0;">다모아</h1>
        <p style="color: #666; font-size: 14px; margin-top: 8px;">인테리어 전문가 매칭 플랫폼</p>
    </div>

    <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); border-radius: 12px; padding: 30px; color: white; text-align: center; margin-bottom: 30px;">
        <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600;">비밀번호 재설정</h2>
        <div style="background: rgba(255,255,255,0.95); border-radius: 8px; padding: 20px; margin: 20px 0;">
            <p style="margin: 0; color: #333; font-size: 14px; margin-bottom: 12px;">아래 인증 코드를 입력해주세요</p>
            <div style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #f5576c; font-family: Consolas, Monaco, monospace;">
                {{verificationCode}}
            </div>
        </div>
        <p style="margin: 16px 0 0 0; font-size: 14px; opacity: 0.9;">유효시간: {{expiryMinutes}}분</p>
    </div>

    <div style="background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px; padding: 16px; margin-bottom: 20px;">
        <p style="margin: 0; color: #856404; font-size: 14px; line-height: 1.6;">
            <strong>⚠️ 보안 안내</strong><br>
            본인이 요청하지 않은 경우, 즉시 고객센터로 연락해주시기 바랍니다.
        </p>
    </div>

    <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e0e0e0; color: #999; font-size: 12px;">
        <p style="margin: 0 0 8px 0;">본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.</p>
        <p style="margin: 0;">© 2025 다모아(Damoa). All rights reserved.</p>
    </div>
</div>',
    '{"verificationCode": "6자리 숫자 인증 코드", "expiryMinutes": "만료 시간(분)"}'::jsonb,
    true,
    '비밀번호 재설정 시 이메일 인증 코드 발송용 템플릿',
    false,
    NULL,
    '{}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
