<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create account — GlobalTrade Logistics</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-md">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
            <h1 class="mb-1 text-xl font-semibold text-gray-900">Create account</h1>
            <p class="mb-6 text-sm text-gray-500">Already have an account? <a href="/auth/login.jsp" class="font-medium text-green-700 hover:underline">Log in</a></p>

            <div id="alert-error" class="mb-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>

            <form id="sign-up-form" class="space-y-4">
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">Email</label>
                    <input type="email" id="email" required
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                </div>
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">Country</label>
                    <select id="country" required
                            class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                        <option value="">Select a country&hellip;</option>
                    </select>
                </div>
                <p class="text-xs text-gray-500">You'll fill in your name, mobile, and address after you sign in for the first time.</p>
                <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Create account</button>
            </form>
        </div>
    </div>
</main>

<script>
    (async function loadCountries() {
        try {
            const res = await fetch("/api/v1/countries");
            const countries = await res.json();
            const select = document.getElementById("country");
            countries.forEach(function (c) {
                const option = document.createElement("option");
                option.value = c.name;
                option.textContent = c.name;
                select.appendChild(option);
            });
        } catch (err) {
            console.error("Could not load countries", err);
        }
    })();

    document.getElementById("sign-up-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const errorEl = document.getElementById("alert-error");
        errorEl.classList.add("hidden");

        const body = {
            email: document.getElementById("email").value,
            country: document.getElementById("country").value,
        };

        try {
            const res = await fetch("/api/v1/auth/signup/customer", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(body),
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.error || ("status " + res.status));
            }
            localStorage.setItem("gtl.customer.session", JSON.stringify(data));
            window.location.href = "/me/update-profile.jsp";
        } catch (err) {
            errorEl.textContent = "Could not create account: " + err.message;
            errorEl.classList.remove("hidden");
        }
    });
</script>
</body>
</html>
