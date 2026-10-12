# MOBITIK Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Global Default Deny**: Any path not explicitly matched is unconditionally denied (`allow read, write: if false`).
2. **Authentication Gate**: Every read and write across all collections requires `request.auth != null`.
3. **PII Isolation**: `/users/{userId}` contains sensitive user profile data (`email`, `pinHash`) and is strictly readable only by the document owner (`request.auth.uid == userId`) or a verified administrator (`isAdmin()`). Public lookups (leaderboard, referral code resolution, wallet address resolution) use `/public_profiles/{userId}`, which contains zero PII.
4. **Immutable Identity & Creation Fields**: `userId`, `walletAddress`, `referralCode`, and `createdAt` cannot be modified after creation (`incoming().field == existing().field`).
5. **Privilege Escalation Prevention**: Users cannot assign `role: 'ADMIN'` during profile creation or update unless `isAdmin()` is true. Email-based admin checks require `request.auth.token.email_verified == true`.
6. **Terminal State Locking**: Mining sessions with `status == 'CLAIMED'` cannot be updated by non-admin users, preventing duplicate reward claims.
7. **Self-Referral Prevention**: `/referrals/{referralId}` requires `incoming().referredUserId == request.auth.uid` and `incoming().referrerUserId != request.auth.uid`.
8. **Query Enforcer**: Every `allow list` rule validates `resource.data` against ownership (`resource.data.userId == request.auth.uid`), public active status (`resource.data.accountStatus == 'ACTIVE'`), or `isAdmin()`.
9. **Temporal Integrity**: All timestamps use server timestamps validated via `is timestamp && <= request.time`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: Reading `/users/alice_123` with `auth == null` -> REJECT.
2. **Cross-User PII Read**: Authenticated `bob_456` reading `/users/alice_123` -> REJECT.
3. **Self-Assigned Admin Role**: Creating `/users/alice_123` with `role: "ADMIN"` as non-admin -> REJECT.
4. **Shadow Field Injection**: Updating `/users/alice_123` with an undeclared field `isVerified: true` -> REJECT.
5. **Email Spoofing Attack**: Calling admin endpoints with `email: "cooleyjohn660@gmail.com"` but `email_verified: false` -> REJECT.
6. **Duplicate Mining Claim (Terminal State Mutation)**: Updating `/mining_sessions/s1` when `existing().status == "CLAIMED"` -> REJECT.
7. **Cross-User Mining Session Creation**: Creating `/mining_sessions/s1` with `userId: "bob_456"` while signed in as `alice_123` -> REJECT.
8. **Self-Referral Attack**: Creating `/referrals/r1` where `referrerUserId == referredUserId` -> REJECT.
9. **Value Poisoning (Oversized String)**: Creating `/support_tickets/t1` with a 10,000-character `subject` -> REJECT.
10. **Immutable Field Tampering**: Updating `/users/alice_123` by changing `walletAddress` or `createdAt` -> REJECT.
11. **Unfiltered Collection Scraping**: Listing `/mining_sessions` or `/transactions` without `.where("userId", "==", uid)` -> REJECT.
12. **Unauthorized FAQ/Config Mutation**: Non-admin user attempting to update `/app_config/global` or delete `/faq_entries/f1` -> REJECT.
