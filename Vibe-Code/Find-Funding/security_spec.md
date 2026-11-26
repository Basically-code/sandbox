# Security Specification & Invariants

## Data Invariants
1. Students can only read and write their own profile information (`/users/{userId}/profile/info`).
2. An unauthenticated user cannot access any student profile or application tracking record.
3. Students cannot access or modify another student's applications or reminders (`userId == request.auth.uid`).
4. Scholarships are public read-only (`/scholarships/{scholarshipId}`) for discovery, but client writes are blocked.
5. All IDs must conform to `isValidId` (`^[a-zA-Z0-9_\-]+$`, length <= 128).
6. String fields are length-bounded to prevent Denial of Wallet payload attacks.

## The Dirty Dozen Payloads (Expected to Fail / PERMISSION_DENIED)
1. **Unauthenticated Read on Profile**: `GET /users/user123/profile/info` without auth -> DENIED.
2. **Cross-User Profile Read**: `GET /users/user123/profile/info` with `auth.uid = 'user456'` -> DENIED.
3. **Cross-User Profile Write**: `SET /users/user123/profile/info` with `auth.uid = 'user456'` -> DENIED.
4. **UID Spoofing**: `SET /users/user123/profile/info` where payload `uid = 'user456'` -> DENIED.
5. **Junk ID Injection**: `GET /users/user%20bad%20id!@#/profile/info` -> DENIED.
6. **Cross-User Application Tracking Write**: `SET /users/user123/applications/app1` with `auth.uid = 'user456'` -> DENIED.
7. **Cross-User Reminder Read**: `GET /users/user123/reminders/rem1` with `auth.uid = 'user456'` -> DENIED.
8. **Client Write on Scholarships**: `SET /scholarships/sch1` by ordinary student -> DENIED.
9. **Client Delete on Scholarships**: `DELETE /scholarships/sch1` by student -> DENIED.
10. **Application Record with Mismatched userId**: `SET /users/user123/applications/app1` where `data.userId = 'attacker'` -> DENIED.
11. **Reminder with Mismatched userId**: `SET /users/user123/reminders/rem1` where `data.userId = 'attacker'` -> DENIED.
12. **Oversized Field Denial of Wallet Attack**: Setting 50KB notes in application tracker -> DENIED.
