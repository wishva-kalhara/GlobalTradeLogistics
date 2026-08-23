<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Application User Management — Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<main class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <div class="flex items-center justify-between">
            <h1 class="text-xl font-semibold text-gray-900">Application User Management</h1>
            <button id="onboard-btn" class="rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700">Onboard User</button>
        </div>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="alert-info" class="mt-4 hidden rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700"></div>

        <div class="mt-6 overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200 text-sm">
                <thead>
                <tr class="text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                    <th class="py-2 pr-4">Email</th>
                    <th class="py-2 pr-4">Full name</th>
                    <th class="py-2 pr-4">Role</th>
                </tr>
                </thead>
                <tbody id="users-table-body" class="divide-y divide-gray-100"></tbody>
            </table>
        </div>
    </div>
</main>

<!-- Onboard User modal -->
<div id="onboard-modal" class="fixed inset-0 hidden items-center justify-center bg-black/30 p-4">
    <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-lg">
        <h2 class="mb-4 text-lg font-semibold text-gray-900">Onboard User</h2>

        <div id="modal-alert-error" class="mb-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>

        <form id="onboard-form" class="space-y-4">
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Email</label>
                <input type="email" id="onboard-email" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Full name</label>
                <input type="text" id="onboard-fullName" required
                       class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30"/>
            </div>
            <div>
                <label class="mb-1 block text-sm font-medium text-gray-700">Role</label>
                <select id="onboard-role" required
                        class="block w-full rounded-md border border-gray-300 px-3 py-2 shadow-sm focus:border-green-500 focus:outline-none focus:ring-2 focus:ring-green-500/30">
                    <option value="ADMIN">ADMIN</option>
                    <option value="WORKER">WORKER</option>
                    <option value="COORDINATOR">COORDINATOR</option>
                    <option value="CUSTOMS_AGENT">CUSTOMS_AGENT</option>
                    <option value="WAREHOUSE_MANAGER">WAREHOUSE_MANAGER</option>
                </select>
            </div>
            <div class="flex justify-end gap-3 pt-2">
                <button type="button" id="onboard-cancel-btn" class="rounded-md border border-gray-300 px-4 py-2 font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
                <button type="submit" class="rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700">Onboard User</button>
            </div>
        </form>
    </div>
</div>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (session.role !== "ADMIN") {
        window.location.href = "/app/access-denied.jsp";
    }

    function showAlert(message) {
        const el = document.getElementById("alert-error");
        el.textContent = message;
        el.classList.remove("hidden");
    }

    function showInfo(message) {
        const el = document.getElementById("alert-info");
        el.textContent = message;
        el.classList.remove("hidden");
        document.getElementById("alert-error").classList.add("hidden");
    }

    async function loadUsers() {
        try {
            const res = await fetch("/api/v1/admin/users", {
                headers: { "Authorization": "Bearer " + session.token },
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/login.jsp";
                return;
            }
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.error || ("status " + res.status));
            }
            const users = await res.json();
            const body = document.getElementById("users-table-body");
            body.innerHTML = "";
            users.forEach(function (u) {
                const row = document.createElement("tr");
                row.innerHTML =
                    "<td class=\"py-2 pr-4 text-gray-900\">" + u.email + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-700\">" + u.fullName + "</td>" +
                    "<td class=\"py-2 pr-4\"><span class=\"inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800\">" + u.role + "</span></td>";
                body.appendChild(row);
            });
        } catch (err) {
            showAlert("Could not load users: " + err.message);
        }
    }

    const modal = document.getElementById("onboard-modal");
    document.getElementById("onboard-btn").addEventListener("click", function () {
        document.getElementById("modal-alert-error").classList.add("hidden");
        document.getElementById("onboard-form").reset();
        modal.classList.remove("hidden");
        modal.classList.add("flex");
    });
    document.getElementById("onboard-cancel-btn").addEventListener("click", function () {
        modal.classList.add("hidden");
        modal.classList.remove("flex");
    });

    document.getElementById("onboard-form").addEventListener("submit", async function (e) {
        e.preventDefault();
        const modalError = document.getElementById("modal-alert-error");
        modalError.classList.add("hidden");

        const body = {
            email: document.getElementById("onboard-email").value,
            fullName: document.getElementById("onboard-fullName").value,
            role: document.getElementById("onboard-role").value,
        };

        try {
            const res = await fetch("/api/v1/admin/users", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": "Bearer " + session.token,
                },
                body: JSON.stringify(body),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.error || ("status " + res.status));
            }
            modal.classList.add("hidden");
            modal.classList.remove("flex");
            showInfo("User " + body.email + " onboarded — an onboarding email has been queued.");
            await loadUsers();
        } catch (err) {
            modalError.textContent = "Could not onboard user: " + err.message;
            modalError.classList.remove("hidden");
        }
    });

    loadUsers();
</script>
</body>
</html>
