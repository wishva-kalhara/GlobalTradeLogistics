<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Log in — Seller Portal</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-md">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
            <h1 class="mb-1 text-xl font-semibold text-gray-900">Log in</h1>
            <p class="mb-6 text-sm text-gray-500">Passwordless sign-in with a one-time email code.</p>

            <div id="alert-error" class="mb-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
            <div id="alert-info" class="mb-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

            <form id="email-form" class="space-y-4">
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">Email</label>
                    <input type="email" id="email" required autofocus
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                </div>
                <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Send OTP</button>
            </form>

            <p class="mt-4 text-center text-sm text-gray-500">
                New here? <a href="/seller/auth/sign-up.jsp" class="font-medium text-green-700 hover:underline">Create Account</a>
            </p>

            <form id="code-form" class="mt-6 hidden space-y-4">
                <p class="text-sm text-gray-600">Enter the code sent to <span id="code-email" class="font-medium text-gray-900"></span>.</p>
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">One-time code</label>
                    <input type="text" id="code" required
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 text-gray-900 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                </div>
                <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Verify</button>
            </form>
        </div>
    </div>
</main>

<script>
    function showAlert(type, message) {
        const errorEl = document.getElementById("alert-error");
        const infoEl = document.getElementById("alert-info");
        if (type === "error") {
            errorEl.textContent = message;
            errorEl.classList.remove("hidden");
            infoEl.classList.add("hidden");
        } else {
            infoEl.textContent = message;
            infoEl.classList.remove("hidden");
            errorEl.classList.add("hidden");
        }
    }

    document.getElementById("email-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const email = document.getElementById("email").value;
        try {
            const res = await fetch("/api/v1/auth/otp/request", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.error || ("status " + res.status));
            }
            showAlert("info", "A one-time code was sent to " + email + ".");
            document.getElementById("email-form").classList.add("hidden");
            document.getElementById("code-form").classList.remove("hidden");
            document.getElementById("code-email").textContent = email;
        } catch (err) {
            showAlert("error", "Could not send code: " + err.message);
        }
    });

    document.getElementById("code-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const email = document.getElementById("code-email").textContent;
        const code = document.getElementById("code").value;
        try {
            const res = await fetch("/api/v1/auth/otp/verify", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email, code: code }),
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            localStorage.setItem("gtl.seller.session", JSON.stringify(data));
            window.location.href = "/seller/me/update-profile.jsp";
        } catch (err) {
            showAlert("error", "Could not verify code: " + err.message);
        }
    });
</script>
</body>
</html>
