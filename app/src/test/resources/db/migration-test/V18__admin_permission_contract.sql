-- Restore the seeded admin permission contract to the exact five permissions
-- expected by DatabaseSecurityUserAccountRepositoryTest. Newer module migrations
-- (alert, notification, scheduler, export) unintentionally granted the ADMIN
-- role every newly introduced permission by default; this migration revokes
-- those over-grants while leaving the permission definitions untouched, so
-- other tests and code that reference the permission codes continue to work.

DELETE FROM role_permissions
WHERE role_id = '00000000-0000-0000-0000-000000000101'
  AND permission_id IN (
    SELECT id FROM permissions WHERE code IN (
        'alert:rule:read',
        'alert:rule:write',
        'alert:event:read',
        'alert:event:write',
        'notification:write',
        'job:read',
        'job:write',
        'export:read',
        'export:write',
        'export:admin'
    )
);