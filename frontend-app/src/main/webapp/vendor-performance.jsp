<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vendor Performance &mdash; Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<%@ include file="/WEB-INF/includes/nav.jsp" %>
<main class="mx-auto max-w-4xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
        <h1 class="text-xl font-semibold text-gray-900">Vendor Performance Report</h1>
        <p class="mt-1 text-sm text-gray-500">On-time delivery scoring, recomputed weekly per supplier.</p>

        <div id="alert-error" class="mt-4 hidden rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"></div>
        <div id="empty-state" class="mt-6 hidden text-sm text-gray-500">No vendor performance reports yet &mdash; they're generated automatically once a week.</div>

        <div class="mt-6 overflow-x-auto">
            <table class="min-w-full divide-y divide-gray-200 text-sm">
                <thead>
                <tr class="text-left text-xs font-medium uppercase tracking-wide text-gray-500">
                    <th class="py-2 pr-4">Supplier ID</th>
                    <th class="py-2 pr-4">Recorded</th>
                    <th class="py-2 pr-4">Summary</th>
                </tr>
                </thead>
                <tbody id="reports-table-body" class="divide-y divide-gray-100"></tbody>
            </table>
        </div>
    </div>
</main>

<script>
    const session = JSON.parse(localStorage.getItem("gtl.app.session") || "null");
    if (!session || !session.token) {
        window.location.href = "/app/login.jsp";
    } else if (session.role !== "ADMIN" && session.role !== "COORDINATOR") {
        window.location.href = "/app/access-denied.jsp";
    }

    (async function loadReports() {
        const errorEl = document.getElementById("alert-error");
        try {
            const res = await fetch("/api/v1/admin/vendor-performance", {
                headers: { "Authorization": "Bearer " + session.token },
            });
            if (res.status === 401) {
                localStorage.removeItem("gtl.app.session");
                window.location.href = "/app/login.jsp";
                return;
            }
            if (!res.ok) {
                const data = await res.json().catch(function () { return {}; });
                throw new Error(data.error || ("status " + res.status));
            }
            const reports = await res.json();
            if (reports.length === 0) {
                document.getElementById("empty-state").classList.remove("hidden");
                return;
            }

            const body = document.getElementById("reports-table-body");
            reports.forEach(function (r) {
                const row = document.createElement("tr");
                row.innerHTML =
                    "<td class=\"py-2 pr-4 text-gray-900\">" + r.reference + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-500\">" + new Date(r.createdAt).toLocaleString() + "</td>" +
                    "<td class=\"py-2 pr-4 text-gray-700\">" + (r.details || "") + "</td>";
                body.appendChild(row);
            });
        } catch (err) {
            errorEl.textContent = "Could not load vendor performance reports: " + err.message;
            errorEl.classList.remove("hidden");
        }
    })();
</script>
</body>
</html>
