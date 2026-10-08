"""HTTP regression tests against a running Tomcat app; Python standard library only.

Run from the repo root: python backend/tests/test_login_logout.py
Uses the local accounts in DataSeeder. Override AUTH_BASE_URL, AUTH_CUSTOMER_EMAIL,
AUTH_CUSTOMER_PASSWORD, AUTH_ADMIN_EMAIL and AUTH_ADMIN_PASSWORD when needed.
No account, cart, product or configuration is modified by these tests.
"""

import copy
import http.cookiejar
import os
import unittest
import urllib.error
import urllib.parse
import urllib.request


BASE = os.environ.get("AUTH_BASE_URL", "http://localhost:8080/ecommerce").rstrip("/")
CUSTOMER_EMAIL = os.environ.get("AUTH_CUSTOMER_EMAIL", "khach@nongviet.vn")
CUSTOMER_PASSWORD = os.environ.get("AUTH_CUSTOMER_PASSWORD", "Khach@123")
ADMIN_EMAIL = os.environ.get("AUTH_ADMIN_EMAIL", "admin@nongviet.vn")
ADMIN_PASSWORD = os.environ.get("AUTH_ADMIN_PASSWORD", "Admin@123")


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Client:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(self.cookies), NoRedirect()
        )

    def request(self, path, data=None, headers=None):
        payload = None if data is None else urllib.parse.urlencode(data).encode("utf-8")
        req = urllib.request.Request(BASE + path, data=payload, headers=headers or {})
        try:
            response = self.opener.open(req, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            return response.code, response.headers, response.read().decode("utf-8")

    def session_id(self):
        return next((c.value for c in self.cookies if c.name == "JSESSIONID"), None)

    def login(self, **extra):
        return self.request("/login", {"email": CUSTOMER_EMAIL, "password": CUSTOMER_PASSWORD, **extra})


class LoginLogoutTests(unittest.TestCase):
    def test_mockup_controls_and_form_action(self):
        status, headers, body = Client().request("/login")
        self.assertEqual(status, 200)
        self.assertEqual(headers.get("Cache-Control"), "no-store")
        for text in ("Chào mừng trở lại!", "Ghi nhớ đăng nhập", "Quên mật khẩu?", "Đăng nhập"):
            self.assertIn(text, body)
        self.assertIn('name="remember"', body)
        self.assertIn('autocomplete="current-password"', body)
        self.assertIn('method="post"', body)

    def test_wrong_missing_and_unknown_credentials_have_same_error(self):
        error = "Email hoặc mật khẩu không đúng."
        cases = [
            {"email": CUSTOMER_EMAIL, "password": "wrong-password"},
            {"email": "unknown-auth-test@nongviet.invalid", "password": "wrong-password"},
            {"email": "", "password": ""},
            {},
        ]
        for data in cases:
            with self.subTest(case=list(data.keys())):
                client = Client()
                status, _, body = client.request("/login", data)
                self.assertEqual(status, 200)
                self.assertIn(error, body)
                self.assertEqual(client.request("/cart")[0], 302)

    def test_failed_form_preserves_email_and_remember_but_not_password(self):
        marker = "wrong-password-must-not-return"
        status, _, body = Client().request("/login", {
            "email": CUSTOMER_EMAIL, "password": marker, "remember": "on", "next": "/cart"
        })
        self.assertEqual(status, 200)
        self.assertIn(CUSTOMER_EMAIL, body)
        self.assertNotIn(marker, body)
        self.assertRegex(body, r'name="remember"[^>]*checked')
        self.assertIn('name="next" value="/cart"', body)

    def test_user_input_is_escaped_on_failed_form(self):
        payload = '"><script>alert(1)</script>'
        status, _, body = Client().request("/login", {"email": payload, "password": "wrong", "next": payload})
        self.assertEqual(status, 200)
        self.assertNotIn(payload, body)
        self.assertIn("&lt;script&gt;", body)

    def test_normal_login_replaces_old_session_and_returns_to_cart(self):
        client = Client()
        client.request("/login")
        old_id = client.session_id()
        status, headers, _ = client.login(email="  " + CUSTOMER_EMAIL.upper() + "  ", next="/cart")
        self.assertEqual(status, 302)
        self.assertTrue(headers["Location"].endswith("/ecommerce/cart"))
        self.assertNotEqual(old_id, client.session_id())
        cookies = headers.get_all("Set-Cookie", [])
        self.assertTrue(any("HttpOnly" in c and "SameSite=Lax" in c for c in cookies))
        self.assertFalse(any("Max-Age=604800" in c for c in cookies))
        self.assertEqual(client.request("/cart")[0], 200)
        client.cookies.clear_session_cookies()
        self.assertEqual(client.request("/cart")[0], 302)

    def test_unsafe_next_falls_back_to_home(self):
        for target in ("https://example.invalid", "//example.invalid", "/\\example.invalid", "/cart\r\nX-Test: injected"):
            with self.subTest(target=repr(target)):
                status, headers, _ = Client().login(next=target)
                self.assertEqual(status, 302)
                self.assertTrue(headers["Location"].endswith("/ecommerce/"))

    def test_remember_cookie_survives_browser_session_cookie_cleanup(self):
        client = Client()
        status, headers, _ = client.login(remember="on")
        self.assertEqual(status, 302)
        self.assertTrue(any("Max-Age=604800" in c for c in headers.get_all("Set-Cookie", [])))
        client.cookies.clear_session_cookies()
        self.assertIsNotNone(client.session_id())
        self.assertEqual(client.request("/cart")[0], 200)

    def test_logout_revokes_remembered_session_and_cookie(self):
        client = Client()
        client.login(remember="on")
        old_cookies = [copy.copy(cookie) for cookie in client.cookies]
        status, headers, _ = client.request("/logout", {})
        self.assertEqual(status, 302)
        self.assertTrue(headers["Location"].endswith("/ecommerce/"))
        self.assertEqual(headers.get("Cache-Control"), "no-store")
        self.assertTrue(any("Max-Age=0" in c for c in headers.get_all("Set-Cookie", [])))
        self.assertIsNone(client.session_id())
        self.assertEqual(client.request("/cart")[0], 302)
        stale = Client()
        for cookie in old_cookies:
            stale.cookies.set_cookie(cookie)
        self.assertEqual(stale.request("/cart")[0], 302)

    def test_get_logout_is_rejected_without_logging_out(self):
        client = Client()
        client.login()
        self.assertEqual(client.request("/logout")[0], 405)
        self.assertEqual(client.request("/cart")[0], 200)

    def test_switching_to_admin_revokes_customer_session(self):
        client = Client()
        client.login(remember="on")
        old_cookies = [copy.copy(cookie) for cookie in client.cookies]
        old_id = client.session_id()
        status, headers, _ = client.request("/login", {"email": ADMIN_EMAIL, "password": ADMIN_PASSWORD})
        self.assertEqual(status, 302)
        self.assertNotEqual(old_id, client.session_id())
        self.assertFalse(any("Max-Age=604800" in c for c in headers.get_all("Set-Cookie", [])))
        self.assertEqual(client.request("/cart")[0], 403)
        stale = Client()
        for cookie in old_cookies:
            stale.cookies.set_cookie(cookie)
        self.assertEqual(stale.request("/cart")[0], 302)

    def test_guest_cart_redirects_normally_and_with_htmx(self):
        status, headers, _ = Client().request("/cart")
        self.assertEqual(status, 302)
        self.assertIn("/login?next=", headers["Location"])
        status, headers, _ = Client().request("/cart", headers={"HX-Request": "true"})
        self.assertEqual(status, 200)
        self.assertIn("/login?next=", headers["HX-Redirect"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
