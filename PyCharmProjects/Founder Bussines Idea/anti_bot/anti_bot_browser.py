import os
import random
import time
from typing import List, Dict, Optional, Any
from dotenv import load_dotenv
from playwright.sync_api import sync_playwright, Browser, BrowserContext, Page
from util.config_loader import get_proxy_list

# Load environment variables (e.g., PROXY_LIST_JSON)
load_dotenv()

class AntiBotBrowser:
    """
    AntiBotBrowser uses Playwright to open a browser with optional proxy and stealth.
    It randomizes User-Agent, injects stealth script if available, and simulates human behavior.
    Use as a context manager to ensure proper cleanup of resources.
    """
    # List of User-Agent strings to randomize selection
    USER_AGENTS: List[str] = [
        # Windows 10 Chrome
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/114.0.5735.199 Safari/537.36",
        # Windows 10 Edge (Chromium)
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/116.0.5845.96 Safari/537.36 Edg/116.0.1938.62",
        # macOS Safari
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_3) AppleWebKit/605.1.15 "
        "(KHTML, like Gecko) Version/16.1 Safari/605.1.15",
        # iPhone Safari
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 "
        "(KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1",
        # Android Chrome (Samsung)
        "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/114.0.5735.131 Mobile Safari/537.36",
        # Firefox on Windows 10 (version 102)
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:102.0) Gecko/20100101 Firefox/102.0",
        # Firefox on Windows 10 (version 85)
        "Mozilla/5.0 (Windows NT 10.0; WOW64; rv:85.0) Gecko/20100101 Firefox/85.0",
        # Firefox on macOS
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 12_6) Gecko/20100101 Firefox/102.0",
        # Firefox on Linux (Ubuntu)
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/109.0",
        # Chrome on Linux (Ubuntu)
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/107.0.5304.87 Safari/537.36",
        # Chrome on iPad (CriOS indicates Chrome on iOS)
        "Mozilla/5.0 (iPad; CPU OS 15_5 like Mac OS X) AppleWebKit/605.1.15 "
        "(KHTML, like Gecko) CriOS/102.0.5005.99 Mobile/15E148 Safari/604.1",
        # Opera on Linux
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/98.0.4758.80 Safari/537.36 OPR/84.0.4316.14",
        # Chrome on Windows (older version 104)
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/104.0.5112.102 Safari/537.36",
        # Chrome on macOS (older version 101)
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_7_2) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/101.0.4951.64 Safari/537.36",
        # Chrome on Android (Pixel 6, Chrome 103)
        "Mozilla/5.0 (Linux; Android 12; Pixel 6 Pro Build/SQ3A.220705.003) "
        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Mobile Safari/537.36",
        # Samsung Browser on Android
        "Mozilla/5.0 (Linux; Android 12; SAMSUNG SM-G991B) AppleWebKit/537.36 "
        "(KHTML, like Gecko) SamsungBrowser/17.0 Chrome/96.0.4664.104 Mobile Safari/537.36",
        # Chrome on Windows (ARM64, Chrome 110)
        "Mozilla/5.0 (Windows NT 10.0; ARM64; WOW64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/110.0.5481.178 Safari/537.36",
        # Safari on macOS (older version 15.1)
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
        "(KHTML, like Gecko) Version/15.1 Safari/605.1.15",
        # Firefox on Linux (older version 91)
        "Mozilla/5.0 (X11; Linux i686; rv:91.0) Gecko/20100101 Firefox/91.0",
        # Yandex Browser on Windows 11 (Chrome 126 base)
        "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/126.0.6478.234 YaBrowser/24.7.1.1076 Yowser/2.5 Safari/537.36"
    ]

    def __init__(self, headless: bool = True, browser_type: str = "chromium",
                 proxy_list: Optional[List[Dict[str, Optional[str]]]] = None,
                 user_agents: Optional[List[str]] = None):
        """
        Initialize AntiBotBrowser.
        :param headless: Whether to run browser in headless mode.
        :param browser_type: Browser type to use ('chromium', 'firefox', or 'webkit').
        :param proxy_list: List of proxy configurations (each a dict with keys 'server', 'username', 'password').
                            If None, will use get_proxy_list() from environment.
        :param user_agents: List of User-Agent strings to choose from. If None, uses default USER_AGENTS list.
        """
        # Type hint for attributes
        self.browser: Optional[Browser] = None
        self.context: Optional[BrowserContext] = None
        self.page: Optional[Page] = None
        self.playwright = None  # Playwright instance (sync_playwright().start())
        self.headless = headless
        self.browser_type = browser_type
        # Use provided proxies or fetch from environment
        self.proxies = proxy_list if proxy_list is not None else get_proxy_list()
        # Ensure at least one entry (None) if list is empty
        if not self.proxies:
            self.proxies = [None]
        # Use provided user-agent list or default
        self.user_agents_list = user_agents if user_agents is not None else self.USER_AGENTS

    def __enter__(self) -> "AntiBotBrowser":
        """
        Enter context: start Playwright, launch browser with proxy (if any), open a new context and page.
        Tries proxies from the list until success (checks connectivity via _get_current_ip).
        """
        # Start Playwright
        self.playwright = sync_playwright().start()
        # Iterate over proxies list to find a working proxy (or no proxy)
        for proxy in self.proxies:
            # Prepare proxy settings for Playwright
            proxy_settings: Optional[Dict[str, str]] = None
            if proxy:
                # proxy is a dict with 'server' and optional 'username','password'
                if "server" in proxy and proxy["server"]:
                    proxy_settings = {
                        "server": proxy["server"]
                    }
                    # Include auth if provided
                    if proxy.get("username") and proxy.get("password"):
                        proxy_settings["username"] = proxy["username"] or ""
                        proxy_settings["password"] = proxy["password"] or ""
            try:
                # Launch the browser with or without proxy settings
                browser_launcher = getattr(self.playwright, self.browser_type)
                if proxy_settings:
                    self.browser = browser_launcher.launch(headless=self.headless, proxy=proxy_settings)
                else:
                    self.browser = browser_launcher.launch(headless=self.headless)
                # Create a new incognito browser context with a random User-Agent
                self.context = self.browser.new_context(user_agent=random.choice(self.user_agents_list))
                # Inject stealth script if available
                stealth_path = "stealth.min.js"
                if os.path.exists(stealth_path):
                    try:
                        self.context.add_init_script(path=stealth_path)
                    except Exception as script_error:
                        # Not critical if stealth script injection fails; log and continue
                        print(f" Не удалось применить stealth-скрипт: {script_error}")
                # Open a new page
                self.page = self.context.new_page()
                # Test connectivity by fetching current IP (via proxy if set)
                ip = self._get_current_ip()
                if ip:
                    # Successfully retrieved IP (proxy works)
                    print(f" Используется IP: {ip}")
                    break
                else:
                    # If no IP was fetched, consider it a failure
                    raise Exception("No IP retrieved (proxy might be invalid).")
            except Exception as e:
                # If any error occurs, close browser and context if opened, then try next proxy
                if self.page:
                    try:
                        self.page.close()
                    except Exception:
                        pass
                    self.page = None
                if self.context:
                    try:
                        self.context.close()
                    except Exception:
                        pass
                    self.context = None
                if self.browser:
                    try:
                        self.browser.close()
                    except Exception:
                        pass
                    self.browser = None
                print(f" Прокси не работает: {proxy} | Ошибка: {e}")
                continue
        if not self.browser or not self.context or not self.page:
            # Stop Playwright if we failed to get any browser running
            if self.playwright:
                try:
                    self.playwright.stop()
                except Exception:
                    pass
                self.playwright = None
            raise Exception("❌ Ошибка: не удалось запустить браузер ни через один прокси")
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """
        Exit context: closes page, context, browser, and stops Playwright.
        Ensures all resources are properly released.
        """
        # Close page, context, browser if they exist
        if self.page is not None:
            try:
                self.page.close()
            except Exception:
                pass
            self.page = None
        if self.context is not None:
            try:
                self.context.close()
            except Exception:
                pass
            self.context = None
        if self.browser is not None:
            try:
                self.browser.close()
            except Exception:
                pass
            self.browser = None
        # Stop the Playwright instance
        if self.playwright is not None:
            try:
                self.playwright.stop()
            except Exception:
                pass
            self.playwright = None

    def goto(self, url: str) -> None:
        """
        Navigate to the specified URL and simulate human interactions on the page.
        If a 429/captcha is detected, reloads the page once and repeats interactions.
        """
        if self.page is None:
            raise Exception("Browser page is not initialized. Did you use the context manager?")
        try:
            self.page.goto(url)
        except Exception as e:
            print(f" Ошибка перехода на URL {url}: {e}")
            return
        # Wait a moment after page load
        self._random_delay(300, 1000)
        # Simulate human behavior (mouse movements, scrolling)
        self._emulate_human()
        # Check for rate limiting or captcha indications
        if self._is_rate_limited():
            print(" Обнаружено ограничение (429). Перезагрузка страницы...")
            try:
                self.page.reload()
            except Exception as e:
                print(f"❌ Ошибка при перезагрузке страницы: {e}")
                return
            # Wait a bit after reload
            self._random_delay(300, 1000)
            # Simulate human behavior again after reload
            self._emulate_human()
            # Check again if still rate-limited
            if self._is_rate_limited():
                print(" Страница по-прежнему ограничена (429) после перезагрузки.")
            else:
                print(" Ограничение снято после перезагрузки.")

    def get_content(self) -> str:
        """
        Get the full HTML content of the current page.
        :return: HTML content as a string.
        """
        if self.page is None:
            raise Exception("Browser page is not initialized.")
        return self.page.content()

    def _is_rate_limited(self) -> bool:
        """
        Determine if the current page content indicates a rate limit or captcha (HTTP 429).
        Looks for typical indicators in the HTML content.
        :return: True if rate-limited content is detected, False otherwise.
        """
        if self.page is None:
            return False
        content = ""
        try:
            content = self.page.content()
        except Exception:
            return False
        # Check for "429" and "That’s an error." (with either typographic or straight apostrophe)
        if "429" in content and ("That\u2019s an error." in content or "That's an error." in content):
            return True
        return False

    def _get_current_ip(self) -> Optional[str]:
        """
        Retrieve the current external IP address by accessing api.ipify.org through the browser.
        Uses a temporary new page so that the main page remains unchanged.
        :return: IP address as string, or None if failed.
        """
        if self.context is None:
            return None
        ip_page: Optional[Page] = None
        try:
            ip_page = self.context.new_page()
            # Use a short timeout for the IP check to avoid long hang on a bad proxy
            ip_page.goto("https://api.ipify.org?format=text", timeout=5000)
            # Get the text content of the page (should be the IP address)
            ip_text = ip_page.text_content("body")
            if ip_text:
                return ip_text.strip()
            else:
                return None
        except Exception:
            return None
        finally:
            # Close the temporary IP page
            if ip_page:
                try:
                    ip_page.close()
                except Exception:
                    pass

    def _inject_google_cookies(self, cookies: Optional[List[Dict[str, Any]]] = None) -> None:
        """
        Inject Google cookies into the browser context.
        If an empty list or None is provided, it will still execute (no-op if empty).
        :param cookies: List of cookie dictionaries to add (keys: name, value, domain, etc.).
        """
        if self.context is None:
            return
        cookies_to_add = cookies if cookies is not None else []
        if not isinstance(cookies_to_add, list):
            # If provided cookies is not a list, do nothing
            print(" Неверный формат cookie, ожидается список словарей.")
            return
        try:
            if cookies_to_add:
                self.context.add_cookies(cookies_to_add)
            else:
                # Even if empty, call add_cookies with empty list (no effect, but for demonstration)
                self.context.add_cookies([])
        except Exception as e:
            print(f" Ошибка при добавлении cookies: {e}")

    def _emulate_human(self) -> None:
        """
        Perform a series of random mouse movements and scroll actions to emulate human behavior.
        """
        if self.page is None:
            return
        try:
            # Get viewport dimensions (if available) or default to a typical screen size
            viewport = self.page.viewport_size or {"width": 1920, "height": 1080}
            width = viewport.get("width", 1920)
            height = viewport.get("height", 1080)
            # Move mouse to a few random positions on the page
            for _ in range(3):
                x = random.randint(0, max(width - 1, 0))
                y = random.randint(0, max(height - 1, 0))
                self.page.mouse.move(x, y)
                self._random_delay(100, 500)
            # Scroll down by a random amount, then up by a smaller random amount
            scroll_down = random.randint(200, 1000)
            self.page.mouse.wheel(0, scroll_down)
            self._random_delay(200, 600)
            scroll_up = random.randint(50, 300)
            self.page.mouse.wheel(0, -scroll_up)
            self._random_delay(100, 300)
        except Exception as e:
            # If emulation fails, just print a warning
            print(f" Ошибка имитации действий человека: {e}")

    @staticmethod
    def _random_delay(min_ms: int = 100, max_ms: int = 300) -> None:
        """
        Sleep for a random duration between min_ms and max_ms milliseconds.
        """
        if max_ms < min_ms:
            max_ms = min_ms
        delay = random.uniform(min_ms, max_ms) / 1000.0
        time.sleep(delay)

    def wait_until_closed(self, message: str = "⏳ Браузер открыт. Закрой окно вручную, чтобы продолжить...") -> None:
        """
        Ждёт, пока пользователь сам не закроет браузер.
        """
        if not self.page or not self.page.context or not self.page.context.browser:
            print(" Браузер не инициализирован.")
            return
        print(message)
        try:
            # Ждёт, пока браузер не будет закрыт вручную
            while True:
                time.sleep(1)
                if self.page.is_closed() or self.page.context.browser.is_connected() is False:
                    print(" Браузер закрыт.")
                    break
        except KeyboardInterrupt:
            print(" Прервано пользователем.")
