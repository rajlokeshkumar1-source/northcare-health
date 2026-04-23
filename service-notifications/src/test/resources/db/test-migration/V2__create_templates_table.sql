-- H2-compatible version: uses RANDOM_UUID() (H2 2.x built-in).
CREATE TABLE notification_templates (
    id            UUID         PRIMARY KEY,
    template_code VARCHAR(100) NOT NULL UNIQUE,
    type          VARCHAR(50)  NOT NULL,
    channel       VARCHAR(20)  NOT NULL,
    subject       VARCHAR(255),
    body          TEXT         NOT NULL,
    is_active     BOOLEAN      DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

INSERT INTO notification_templates (id, template_code, type, channel, subject, body) VALUES
    (RANDOM_UUID(),
     'APPOINTMENT_REMINDER_24H',
     'APPOINTMENT_REMINDER',
     'EMAIL',
     'Reminder: Your appointment tomorrow at NorthCare',
     'Dear {{patientName}}, this is a reminder that you have an appointment on {{appointmentDate}} at {{appointmentTime}} with Dr. {{doctorName}}. Location: {{location}}.'),

    (RANDOM_UUID(),
     'LAB_RESULT_READY',
     'LAB_RESULT_READY',
     'EMAIL',
     'Your lab results are ready',
     'Dear {{patientName}}, your lab results for {{testName}} are now available. Please log in to your patient portal or contact your care team.'),

    (RANDOM_UUID(),
     'PAYMENT_DUE_REMINDER',
     'BILLING_DUE',
     'EMAIL',
     'Payment due: Invoice {{invoiceNumber}}',
     'Dear {{patientName}}, your invoice {{invoiceNumber}} for {{amount}} CAD is due on {{dueDate}}.'),

    (RANDOM_UUID(),
     'EMERGENCY_ALERT',
     'EMERGENCY_ALERT',
     'IN_APP',
     'URGENT: {{alertTitle}}',
     'EMERGENCY ALERT: {{alertMessage}}. Please follow emergency procedures immediately.'),

    (RANDOM_UUID(),
     'PRESCRIPTION_READY',
     'PRESCRIPTION_READY',
     'SMS',
     NULL,
     'NorthCare: Your prescription for {{medicationName}} is ready for pickup at {{pharmacy}}. Ref: {{prescriptionId}}');
