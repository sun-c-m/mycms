-- Never edit an applied Flyway migration. Update the initial account in a new version.
UPDATE cms.user
SET password = '7842509'
WHERE username = 'sun';

UPDATE cms.user
SET username = 'sun', password = '7842509'
WHERE username = 'liwei'
  AND NOT EXISTS (
    SELECT 1
    FROM (SELECT id FROM cms.user WHERE username = 'sun') existing_user
  );
