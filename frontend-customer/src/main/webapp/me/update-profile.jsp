<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Update profile — GlobalTrade Logistics</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-md">
        <div class="rounded-2xl border border-gray-200 bg-white p-8 shadow-sm">
            <h1 class="mb-1 text-xl font-semibold text-gray-900">Update your profile</h1>
            <p class="mb-6 text-sm text-gray-500">Shipping and contact details.</p>

            <div id="alert-error" class="mb-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
            <div id="alert-info" class="mb-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

            <form id="profile-form" class="space-y-4">
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">Full name</label>
                    <input type="text" id="fullName" required
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                </div>
                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="mb-1 block text-sm font-medium text-gray-700">Mobile 1</label>
                        <input type="text" id="mobile1"
                               class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                    </div>
                    <div>
                        <label class="mb-1 block text-sm font-medium text-gray-700">Mobile 2</label>
                        <input type="text" id="mobile2"
                               class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                    </div>
                </div>
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">Address</label>
                    <input type="text" id="address"
                           class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
                </div>
                <div>
                    <label class="mb-1 block text-sm font-medium text-gray-700">Country</label>
                    <select id="country"
                            class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                        <option value="">Select a country&hellip;</option>
                    </select>
                </div>
                <button type="submit" class="w-full rounded-md bg-green-600 px-4 py-2.5 font-medium text-white hover:bg-green-700">Save profile</button>
            </form>
        </div>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.customer.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/auth/login.jsp";
    }

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

    document.getElementById("profile-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const errorEl = document.getElementById("alert-error");
        const infoEl = document.getElementById("alert-info");
        errorEl.classList.add("hidden");
        infoEl.classList.add("hidden");

        const body = {
            fullName: document.getElementById("fullName").value,
            mobile1: document.getElementById("mobile1").value,
            mobile2: document.getElementById("mobile2").value,
            address: document.getElementById("address").value,
            country: document.getElementById("country").value,
        };

        try {
            const res = await fetch("/api/v1/me/customer", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify(body),
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.customer.session");
                window.location.href = "/auth/login.jsp";
                return;
            }
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.error || ("status " + res.status));
            }
            infoEl.textContent = "Profile updated.";
            infoEl.classList.remove("hidden");
        } catch (err) {
            errorEl.textContent = "Could not update profile: " + err.message;
            errorEl.classList.remove("hidden");
        }
    });
</script>
</body>
</html>
