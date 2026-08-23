<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!doctype html>
<html lang="en" class="h-full bg-gray-50">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Access denied — Staff Console</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="h-full">
<main class="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
    <div class="mx-auto max-w-md">
        <div class="rounded-2xl border border-red-200 bg-white p-8 text-center shadow-sm">
            <span class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-xl bg-red-100 text-xl font-bold text-red-600">!</span>
            <h1 class="text-xl font-semibold text-gray-900">Access denied</h1>
            <p class="mt-2 text-sm text-gray-500">Your account does not have permission to view that page.</p>
            <a href="/app/login.jsp"
               class="mt-6 inline-block rounded-md bg-green-600 px-5 py-2.5 font-medium text-white hover:bg-green-700">Back to login</a>
        </div>
    </div>
</main>
</body>
</html>
