-- ===========================================
-- V7: 비밀번호 찾기 및 변경 알림 템플릿 추가
-- ===========================================

-- 1. 비밀번호 찾기 인증 코드 템플릿 (PASSWORD_RESET)
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
    '비밀번호 찾기 인증 코드',
    'PASSWORD_RESET',
    'EMAIL',
    '[다모아] 비밀번호 찾기 인증 코드',
    '<div style="font-family: ''Apple SD Gothic Neo'', ''Malgun Gothic'', ''맑은 고딕'', system-ui, -apple-system, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px;">
    <div style="text-align: center; margin-bottom: 40px;">
        <h1 style="color: #333; font-size: 28px; font-weight: 700; margin: 0;">다모아</h1>
        <p style="color: #666; font-size: 14px; margin-top: 8px;">인테리어 전문가 매칭 플랫폼</p>
    </div>

    <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); border-radius: 12px; padding: 30px; color: white; text-align: center; margin-bottom: 30px;">
        <h2 style="margin: 0 0 16px 0; font-size: 20px; font-weight: 600;">🔑 비밀번호 찾기</h2>
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
            본인이 요청하지 않은 경우, 즉시 고객센터로 연락해주시기 바랍니다.<br>
            이 인증 코드는 비밀번호를 재설정하는데 사용되므로 절대 타인과 공유하지 마세요.
        </p>
    </div>

    <div style="background: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
        <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #333;">💡 안내사항</h3>
        <ul style="margin: 0; padding-left: 20px; color: #666; font-size: 14px; line-height: 1.8;">
            <li>인증 코드는 {{expiryMinutes}}분 후 자동으로 만료됩니다.</li>
            <li>비밀번호 재설정 후 새 비밀번호로 로그인해주세요.</li>
            <li>정기적으로 비밀번호를 변경하여 계정을 안전하게 보호하세요.</li>
        </ul>
    </div>

    <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e0e0e0; color: #999; font-size: 12px;">
        <p style="margin: 0 0 8px 0;">본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.</p>
        <p style="margin: 0;">© 2025 다모아(Damoa). All rights reserved.</p>
    </div>
</div>',
    '{"verificationCode": "6자리 숫자 인증 코드", "expiryMinutes": "만료 시간(분)"}'::jsonb,
    true,
    '비밀번호 찾기 시 이메일 인증 코드 발송용 템플릿',
    false,
    NULL,
    '{}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;

-- 2. 비밀번호 변경 완료 알림 템플릿 (PASSWORD_CHANGED)
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
    '비밀번호 변경 완료 알림',
    'PASSWORD_CHANGED',
    'EMAIL',
    '[다모아] 비밀번호가 변경되었습니다',
    '<div style="font-family: ''Apple SD Gothic Neo'', ''Malgun Gothic'', ''맑은 고딕'', system-ui, -apple-system, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px 20px;">
    <div style="text-align: center; margin-bottom: 40px;">
        <h1 style="color: #333; font-size: 28px; font-weight: 700; margin: 0;">다모아</h1>
        <p style="color: #666; font-size: 14px; margin-top: 8px;">인테리어 전문가 매칭 플랫폼</p>
    </div>

    <div style="background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); border-radius: 12px; padding: 30px; color: white; text-align: center; margin-bottom: 30px;">
        <div style="font-size: 48px; margin-bottom: 16px;">✅</div>
        <h2 style="margin: 0 0 12px 0; font-size: 24px; font-weight: 600;">비밀번호가 변경되었습니다</h2>
        <p style="margin: 0; font-size: 14px; opacity: 0.9;">회원님의 계정 비밀번호가 성공적으로 변경되었습니다.</p>
    </div>

    <div style="background: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
        <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #333;">📋 변경 정보</h3>
        <table style="width: 100%; border-collapse: collapse;">
            <tr>
                <td style="padding: 8px 0; color: #666; font-size: 14px;">변경 일시</td>
                <td style="padding: 8px 0; color: #333; font-size: 14px; font-weight: 600; text-align: right;">{{changedAt}}</td>
            </tr>
            <tr>
                <td style="padding: 8px 0; color: #666; font-size: 14px;">변경 방법</td>
                <td style="padding: 8px 0; color: #333; font-size: 14px; font-weight: 600; text-align: right;">{{changeMethod}}</td>
            </tr>
        </table>
    </div>

    <div style="background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 4px; padding: 16px; margin-bottom: 20px;">
        <p style="margin: 0; color: #856404; font-size: 14px; line-height: 1.6;">
            <strong>⚠️ 본인이 변경하지 않았다면?</strong><br>
            즉시 고객센터(1234-5678)로 연락하여 계정 보안 조치를 받으시기 바랍니다.
        </p>
    </div>

    <div style="background: #e3f2fd; border-radius: 8px; padding: 20px; margin-bottom: 20px;">
        <h3 style="margin: 0 0 12px 0; font-size: 16px; color: #1976d2;">🔒 보안 팁</h3>
        <ul style="margin: 0; padding-left: 20px; color: #555; font-size: 14px; line-height: 1.8;">
            <li>비밀번호는 8자 이상, 영문/숫자/특수문자를 조합하여 사용하세요.</li>
            <li>다른 사이트와 동일한 비밀번호를 사용하지 마세요.</li>
            <li>정기적으로 비밀번호를 변경하여 계정을 안전하게 보호하세요.</li>
            <li>비밀번호를 타인과 공유하지 마세요.</li>
        </ul>
    </div>

    <div style="text-align: center; padding-top: 20px; border-top: 1px solid #e0e0e0; color: #999; font-size: 12px;">
        <p style="margin: 0 0 8px 0;">본 메일은 발신 전용입니다. 문의사항은 고객센터를 이용해주세요.</p>
        <p style="margin: 0;">© 2025 다모아(Damoa). All rights reserved.</p>
    </div>
</div>',
    '{"changedAt": "변경 일시 (YYYY-MM-DD HH:mm:ss)", "changeMethod": "변경 방법 (로그인 후 변경 / 비밀번호 찾기)"}'::jsonb,
    true,
    '비밀번호 변경 완료 시 알림 이메일 발송용 템플릿',
    false,
    NULL,
    '{}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (code) DO NOTHING;
