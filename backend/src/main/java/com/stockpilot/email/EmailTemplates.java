package com.stockpilot.email;

final class EmailTemplates {

    private EmailTemplates() {
    }

    private static final String WRAPPER = """
            <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;padding:24px;color:#1e293b">
              <div style="font-size:20px;font-weight:700;color:#4f46e5;margin-bottom:16px">StockPilot</div>
              %s
              <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0"/>
              <div style="font-size:12px;color:#94a3b8">StockPilot — multi-channel inventory management. This is an automated message.</div>
            </div>
            """;

    static String otp(String heading, String intro, String code) {
        String inner = """
                <h2 style="font-size:18px">%s</h2>
                <p>%s</p>
                <div style="font-size:32px;font-weight:700;letter-spacing:8px;background:#eef2ff;color:#4338ca;
                            padding:16px;text-align:center;border-radius:8px;margin:16px 0">%s</div>
                <p style="color:#64748b">This code expires in 10 minutes. If you didn't request it, you can ignore this email.</p>
                """.formatted(heading, intro, code);
        return WRAPPER.formatted(inner);
    }

    static String welcome(String name) {
        String inner = """
                <h2 style="font-size:18px">Welcome, %s!</h2>
                <p>Your StockPilot account is verified and ready. You can now manage products, allocate
                inventory across channels, record sales, and track your dashboard analytics.</p>
                """.formatted(name);
        return WRAPPER.formatted(inner);
    }

    static String invite(String orgName, String tempInfo) {
        String inner = """
                <h2 style="font-size:18px">You've been invited to %s on StockPilot</h2>
                <p>An administrator has added you to their team. %s</p>
                """.formatted(orgName, tempInfo);
        return WRAPPER.formatted(inner);
    }

    static String lowStock(String orgName, String body) {
        String inner = """
                <h2 style="font-size:18px">Low stock alert — %s</h2>
                <p>The following items have reached or fallen below their reorder level:</p>
                %s
                """.formatted(orgName, body);
        return WRAPPER.formatted(inner);
    }
}
