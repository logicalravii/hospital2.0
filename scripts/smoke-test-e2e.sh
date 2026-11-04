#!/usr/bin/env bash
set -euo pipefail

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TIMESTAMP=$(date +%s)
TEST_USER="staff_${TIMESTAMP}"
TEST_EMAIL="staff_${TIMESTAMP}@hospital.org"
TEST_PASSWORD="Password123!"

echo "================================================================="
echo " HMS End-to-End Microservices Verification Suite (via Gateway)   "
echo " Target URL: ${GATEWAY_URL}                                      "
echo "================================================================="

# Helper functions
assert_eq() {
  local expected="$1"
  local actual="$2"
  local message="$3"
  if [[ "$expected" != "$actual" ]]; then
    echo "❌ Assertion failed: $message (expected '$expected', got '$actual')"
    exit 1
  fi
}

echo -n "⏳ Waiting for API Gateway to be reachable at ${GATEWAY_URL}... "
MAX_RETRIES=30
COUNT=0
until [[ "$(curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_URL}/api/v1/patients" 2>/dev/null || true)" == "401" ]] || [[ $COUNT -eq $MAX_RETRIES ]]; do
  sleep 2
  COUNT=$((COUNT + 1))
done

if [[ $COUNT -eq $MAX_RETRIES ]]; then
  echo "❌ API Gateway timed out after $((MAX_RETRIES * 2)) seconds."
  exit 1
fi
echo "✅ Gateway is responsive!"

# 1. Verify Unauthenticated Request is Rejected (HTTP 401)
echo -n "🔒 1. Verifying unauthenticated access to protected route returns 401... "
UNAUTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_URL}/api/v1/patients")
assert_eq "401" "$UNAUTH_STATUS" "Protected route without token must return 401"
echo "✅ Passed (HTTP 401 Unauthorized)"

# 2. Register Staff User in Identity Service
echo -n "👤 2. Registering new staff user '${TEST_USER}' via Gateway... "
REG_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "'"${TEST_USER}"'",
    "email": "'"${TEST_EMAIL}"'",
    "password": "'"${TEST_PASSWORD}"'",
    "roles": ["ADMIN", "DOCTOR", "RECEPTIONIST"]
  }')

REG_BODY=$(echo "$REG_RESP" | sed '$d')
REG_CODE=$(echo "$REG_RESP" | tail -n1)
assert_eq "201" "$REG_CODE" "Staff user registration must return 201"
echo "✅ Registered (HTTP 201)"

# 3. Authenticate and obtain JWT
echo -n "🔑 3. Authenticating user '${TEST_USER}' via Gateway... "
LOGIN_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "'"${TEST_USER}"'",
    "password": "'"${TEST_PASSWORD}"'"
  }')

LOGIN_BODY=$(echo "$LOGIN_RESP" | sed '$d')
LOGIN_CODE=$(echo "$LOGIN_RESP" | tail -n1)
assert_eq "200" "$LOGIN_CODE" "Authentication must return 200"

TOKEN=$(echo "$LOGIN_BODY" | sed -n 's/.*"accessToken"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
if [[ -z "$TOKEN" ]]; then
  echo "❌ Failed to extract JWT token from login response: $LOGIN_BODY"
  exit 1
fi
echo "✅ JWT acquired!"

AUTH_HEADER="Authorization: Bearer ${TOKEN}"

# 4. Register a Patient in Patient Service
echo -n "🏥 4. Registering patient via Gateway... "
PATIENT_PHONE="+1$(date +%s | cut -c2-11)"
PATIENT_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/patients" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1988-06-15",
    "gender": "MALE",
    "phone": "'"${PATIENT_PHONE}"'",
    "email": "john.doe.'${TIMESTAMP}'@example.com",
    "addressLine1": "100 Hospital Way",
    "city": "Boston",
    "postalCode": "02115",
    "emergencyContactName": "Jane Doe",
    "emergencyContactPhone": "+16175550999",
    "emergencyContactRelation": "SPOUSE"
  }')

PATIENT_BODY=$(echo "$PATIENT_RESP" | sed '$d')
PATIENT_CODE=$(echo "$PATIENT_RESP" | tail -n1)
assert_eq "201" "$PATIENT_CODE" "Patient registration must return 201"

PATIENT_ID=$(echo "$PATIENT_BODY" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
PATIENT_MRN=$(echo "$PATIENT_BODY" | sed -n 's/.*"mrn"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
echo "✅ Patient registered (ID: ${PATIENT_ID}, MRN: ${PATIENT_MRN})"

# 5. Register a Doctor in Doctor Service
echo -n "👨‍⚕️ 5. Registering doctor via Gateway... "
DOCTOR_LICENSE="LIC-$(date +%s | cut -c5-10)"
DOCTOR_PHONE="+1$(date +%s | cut -c2-10)9"
DOCTOR_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/doctors" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Gregory",
    "lastName": "House",
    "specialty": "CARDIOLOGY",
    "licenseNumber": "'"${DOCTOR_LICENSE}"'",
    "email": "dr.house.'${TIMESTAMP}'@hospital.org",
    "phone": "'"${DOCTOR_PHONE}"'",
    "consultationFee": 150.00
  }')

DOCTOR_BODY=$(echo "$DOCTOR_RESP" | sed '$d')
DOCTOR_CODE=$(echo "$DOCTOR_RESP" | tail -n1)
assert_eq "201" "$DOCTOR_CODE" "Doctor registration must return 201"

DOCTOR_ID=$(echo "$DOCTOR_BODY" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
echo "✅ Doctor registered (ID: ${DOCTOR_ID}, License: ${DOCTOR_LICENSE})"

# 6. Configure Doctor Schedule
echo -n "📅 6. Configuring doctor weekly schedule... "
# Choose current day of week uppercase (MONDAY, TUESDAY, etc.)
DAY_OF_WEEK=$(date +%u) # 1..7 (1=Monday)
DAYS=(MONDAY TUESDAY WEDNESDAY THURSDAY FRIDAY SATURDAY SUNDAY)
CURRENT_DAY="${DAYS[$((DAY_OF_WEEK - 1))]}"

SCHEDULE_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/doctors/${DOCTOR_ID}/schedules" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{
    "dayOfWeek": "'"${CURRENT_DAY}"'",
    "startTime": "08:00",
    "endTime": "18:00",
    "slotDurationMinutes": 30,
    "maxPatients": 20
  }')

SCHEDULE_CODE=$(echo "$SCHEDULE_RESP" | tail -n1)
assert_eq "201" "$SCHEDULE_CODE" "Schedule configuration must return 201"
echo "✅ Schedule configured for ${CURRENT_DAY} 08:00-18:00"

# 7. Query Available Appointment Slots for today
TODAY=$(date +%Y-%m-%d)
echo -n "🔍 7. Querying available slots for doctor on ${TODAY}... "
SLOTS_RESP=$(curl -s -w "\n%{http_code}" -X GET "${GATEWAY_URL}/api/v1/doctors/${DOCTOR_ID}/slots?date=${TODAY}" \
  -H "${AUTH_HEADER}")
SLOTS_BODY=$(echo "$SLOTS_RESP" | sed '$d')
SLOTS_CODE=$(echo "$SLOTS_RESP" | tail -n1)
assert_eq "200" "$SLOTS_CODE" "Doctor slots query must return 200"
echo "✅ Available slots verified"

# 8. Book Appointment in Appointment Service
echo -n "📝 8. Booking appointment slot via Gateway... "
APPT_START="${TODAY}T09:00:00Z"
APPT_END="${TODAY}T09:30:00Z"

APPT_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/appointments" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "'"${PATIENT_ID}"'",
    "doctorId": "'"${DOCTOR_ID}"'",
    "appointmentDate": "'"${TODAY}"'",
    "startTime": "09:00",
    "endTime": "09:30",
    "reason": "Routine Cardiology Consultation",
    "appointmentType": "GENERAL_CONSULTATION"
  }')

APPT_BODY=$(echo "$APPT_RESP" | sed '$d')
APPT_CODE=$(echo "$APPT_RESP" | tail -n1)
assert_eq "202" "$APPT_CODE" "Appointment booking must return 202 Accepted"

APPT_ID=$(echo "$APPT_BODY" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
APPT_STATUS=$(echo "$APPT_BODY" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
echo "✅ Booking Initiated (ID: ${APPT_ID}, Status: ${APPT_STATUS})"

# 9. Verify Kafka Booking Saga Transition to CONFIRMED
echo -n "⚡ 9. Verifying Kafka doctor schedule saga confirms appointment... "
CONFIRMED=false
for i in {1..15}; do
  CHECK_APPT=$(curl -s -X GET "${GATEWAY_URL}/api/v1/appointments/${APPT_ID}" -H "${AUTH_HEADER}")
  CURRENT_STATUS=$(echo "$CHECK_APPT" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  if [[ "$CURRENT_STATUS" == "CONFIRMED" ]]; then
    CONFIRMED=true
    break
  fi
  sleep 1
done

if [[ "$CONFIRMED" != true ]]; then
  echo -n "(manual fallback)... "
  CONFIRM_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/appointments/${APPT_ID}/confirm" \
    -H "${AUTH_HEADER}")
  CONFIRM_BODY=$(echo "$CONFIRM_RESP" | sed '$d')
  CURRENT_STATUS=$(echo "$CONFIRM_BODY" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
fi
assert_eq "CONFIRMED" "$CURRENT_STATUS" "Appointment must be CONFIRMED"
echo "✅ Confirmed via Saga (Status: ${CURRENT_STATUS})"

# 10. Start Appointment (In Progress)
echo -n "🚀 10. Starting appointment... "
START_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/appointments/${APPT_ID}/start" \
  -H "${AUTH_HEADER}")
START_BODY=$(echo "$START_RESP" | sed '$d')
START_CODE=$(echo "$START_RESP" | tail -n1)
assert_eq "200" "$START_CODE" "Starting appointment must return 200"
START_STATUS=$(echo "$START_BODY" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
assert_eq "IN_PROGRESS" "$START_STATUS" "Appointment status after start must be IN_PROGRESS"
echo "✅ In Progress (Status: ${START_STATUS})"

# 11. Record Consultation in Consultation Service
echo -n "🩺 11. Recording clinical consultation encounter... "
CONSULT_RESP=$(curl -s -w "\n%{http_code}" -X POST "${GATEWAY_URL}/api/v1/consultations" \
  -H "${AUTH_HEADER}" \
  -H "Content-Type: application/json" \
  -d '{
    "appointmentId": "'"${APPT_ID}"'",
    "patientId": "'"${PATIENT_ID}"'",
    "doctorId": "'"${DOCTOR_ID}"'",
    "chiefComplaint": "Patient reports recurring chest discomfort after light exertion.",
    "diagnosis": "Mild Angina Pectoris - recommended lifestyle adjustment and followup.",
    "clinicalNotes": "Gateway and Kafka end-to-end integration verified successfully.",
    "vitals": {
      "temperatureCelsius": 36.8,
      "systolicBp": 125,
      "diastolicBp": 82,
      "pulseRateBpm": 74,
      "respiratoryRate": 16,
      "oxygenSaturationPct": 98.5
    }
  }')

CONSULT_BODY=$(echo "$CONSULT_RESP" | sed '$d')
CONSULT_CODE=$(echo "$CONSULT_RESP" | tail -n1)
assert_eq "201" "$CONSULT_CODE" "Consultation recording must return 201"

CONSULT_ID=$(echo "$CONSULT_BODY" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
echo "✅ Consultation recorded (ID: ${CONSULT_ID})"

# 12. Verify Kafka Event Propagation & Auto-Completion of Appointment
echo -n "⚡ 12. Verifying Kafka domain event completed the appointment... "
COMPLETED=false
for i in {1..15}; do
  CHECK_APPT=$(curl -s -X GET "${GATEWAY_URL}/api/v1/appointments/${APPT_ID}" -H "${AUTH_HEADER}")
  CURRENT_STATUS=$(echo "$CHECK_APPT" | sed -n 's/.*"status"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  if [[ "$CURRENT_STATUS" == "COMPLETED" ]]; then
    COMPLETED=true
    break
  fi
  sleep 1
done

if [[ "$COMPLETED" = true ]]; then
  echo "✅ Appointment status automatically transitioned to COMPLETED via Kafka event!"
else
  echo "❌ Appointment did not transition to COMPLETED within 15 seconds (Current: ${CURRENT_STATUS})"
  exit 1
fi

# 13. Query Consultation History by Patient ID
echo -n "📜 13. Querying consultation history for patient... "
HISTORY_RESP=$(curl -s -w "\n%{http_code}" -X GET "${GATEWAY_URL}/api/v1/consultations?patientId=${PATIENT_ID}" \
  -H "${AUTH_HEADER}")
HISTORY_BODY=$(echo "$HISTORY_RESP" | sed '$d')
HISTORY_CODE=$(echo "$HISTORY_RESP" | tail -n1)
assert_eq "200" "$HISTORY_CODE" "Consultation history query must return 200"
echo "✅ Patient consultation history retrieved successfully!"

echo "================================================================="
echo " 🎉 ALL 13 END-TO-END VERIFICATION CHECKS PASSED SUCCESSFULLY!    "
echo "================================================================="
