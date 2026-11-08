// ==================== 全局状态 ====================
let token = localStorage.getItem('token');
let user = JSON.parse(localStorage.getItem('user') || 'null');
let sessionId = generateSessionId();

// ==================== DOM 元素 ====================
const loginPage = document.getElementById('login-page');
const chatPage = document.getElementById('chat-page');
const loginForm = document.getElementById('login-form');
const chatForm = document.getElementById('chat-form');
const messageInput = document.getElementById('message-input');
const messageList = document.getElementById('message-list');
const logoutBtn = document.getElementById('logout-btn');
const loginError = document.getElementById('login-error');
const userInfo = document.getElementById('user-info');
const sendBtn = document.getElementById('send-btn');

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
bindEvents();
initMarkdown();
});

// 初始化 Markdown 解析器
function initMarkdown() {
    if (typeof marked !== 'undefined') {
        marked.setOptions({
            breaks: true,        // 换行转 <br>
            gfm: true,           // 启用 GitHub 风格 Markdown
            sanitize: false,     // 不 sanitized（我们自己处理 XSS）
            smartLists: true,    // 智能列表
            smartypants: false   // 不使用智能标点
        });
    }
}

function checkAuth() {
    if (token && user) {
        showChatPage();
    } else {
        showLoginPage();
    }
}

function bindEvents() {
    // 登录表单提交
    loginForm.addEventListener('submit', handleLogin);

    // 聊天表单提交
    chatForm.addEventListener('submit', handleChat);

    // 退出登录
    logoutBtn.addEventListener('click', handleLogout);

    // 输入框自适应高度 & 快捷键
    messageInput.addEventListener('input', autoResize);
    messageInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault(); // 阻止默认换行
        chatForm.requestSubmit();
    }
});
}

// ==================== 登录逻辑 ====================
async function handleLogin(e) {
    e.preventDefault();
    loginError.textContent = '';

    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    if (!username || !password) {
        loginError.textContent = '请输入用户名和密码';
        return;
    }

    try {
        const response = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await response.json();

        if (data.code === 200) {
            token = data.token;
            user = data.user;

            // 存储到 localStorage
            localStorage.setItem('token', token);
            localStorage.setItem('user', JSON.stringify(user));

            showChatPage();
        } else {
            loginError.textContent = data.message || '登录失败';
        }
    } catch (error) {
        loginError.textContent = '网络错误，请稍后重试';
        console.error('Login error:', error);
    }
}

// ==================== 聊天逻辑 ====================
async function handleChat(e) {
    e.preventDefault();

    const message = messageInput.value.trim();
    if (!message) return;

    // 清空输入框并恢复高度
    messageInput.value = '';
    messageInput.style.height = 'auto';

    // 禁用发送按钮
    sendBtn.disabled = true;

    // 显示用户消息
    appendMessage('user', message);

    // 显示 AI 加载状态
    const assistantEl = appendMessage('assistant', '');
    const textEl = assistantEl.querySelector('.message-text');
    textEl.innerHTML = `
        <div class="typing-indicator">
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
            <div class="typing-dot"></div>
        </div>
    `;

    try {
        const response = await fetch('/api/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                sessionId: sessionId,
                message: message,
                type: 'chat',
                timestamp: new Date().toISOString()
            })
        });

        // Token 过期处理
        if (response.status === 401) {
            handleLogout();
            alert('登录已过期，请重新登录');
            return;
        }

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // 读取 SSE 流
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullText = '';
        let buffer = '';

        // 清除加载动画
        textEl.textContent = '';

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop(); // 保留不完整行

            for (const line of lines) {
                if (line.startsWith('data:')) {
                    const data = line.substring(5);
                    if (data.length > 0) {
                        fullText += data;
                    } else {
                        fullText += '\n';
                    }
                    renderMarkdown(textEl, fullText);
                    scrollToBottom();
                }
            }
        }

        // 如果没有收到任何内容
        if (!fullText) {
            textEl.textContent = '[小坡走神了，请重试~]';
        }

    } catch (error) {
        textEl.textContent = '[哎呀，网络好像开小差了，请重试~]';
        console.error('Stream error:', error);
    } finally {
        // 恢复发送按钮
        sendBtn.disabled = false;
        messageInput.focus();
        scrollToBottom();
    }
}

// ==================== 退出登录 ====================
function handleLogout() {
    token = null;
    user = null;
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    showLoginPage();
}

// ==================== UI 辅助函数 ====================
function showLoginPage() {
    loginPage.classList.add('active');
    chatPage.classList.remove('active');
    loginForm.reset();
    loginError.textContent = '';
}

function showChatPage() {
    loginPage.classList.remove('active');
    chatPage.classList.add('active');
    if (user) {
        userInfo.textContent = `👤 ${user.username || '用户'}`;
    }
    messageInput.focus();
}

function appendMessage(role, text) {
    const messageEl = document.createElement('div');
    messageEl.className = `message-item ${role}`;

    const avatar = role === 'user' ? '👤' : '🤖';
    const time = getCurrentTime();

    messageEl.innerHTML = `
        <div class="message-avatar">${avatar}</div>
        <div class="message-content">
            <div class="message-text">${renderMarkdownForDisplay(text)}</div>
            <div class="message-time">${time}</div>
        </div>
    `;

    messageList.appendChild(messageEl);
    scrollToBottom();

    return messageEl;
}

function scrollToBottom() {
    messageList.scrollTop = messageList.scrollHeight;
}

function autoResize() {
    messageInput.style.height = 'auto';
    messageInput.style.height = Math.min(messageInput.scrollHeight, 120) + 'px';
}

function getCurrentTime() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 渲染 Markdown 内容（带降级处理）
function renderMarkdown(el, text) {
    if (typeof marked !== 'undefined') {
        el.innerHTML = processMarkdownWithCodeBlocks(text);
        attachCopyHandlers(el);
    } else {
        // 降级：保留换行的纯文本显示
        el.innerHTML = escapeHtml(text).replace(/\n/g, '<br>');
    }
}

// 渲染 Markdown 并返回 HTML 字符串（用于初始化消息）
function renderMarkdownForDisplay(text) {
    if (!text) return '';
    if (typeof marked !== 'undefined') {
        return processMarkdownWithCodeBlocks(text);
    } else {
        return escapeHtml(text).replace(/\n/g, '<br>');
    }
}

// 处理 Markdown 并包装代码块
function processMarkdownWithCodeBlocks(text) {
    // 先解析 Markdown
    const html = marked.parse(text);

    // 用正则替换 pre > code 标签，添加包装容器和复制按钮
    return html.replace(/<pre><code class="language-([^"]+)">([\s\S]*?)<\/code><\/pre>/g,
        function(match, lang, code) {
            return `<div class="code-block-wrapper">
                <div class="code-block-header">
                    <span class="code-lang">${lang}</span>
                    <button class="copy-btn" onclick="copyCode(this)">复制</button>
                </div>
                <pre><code class="language-${lang}">${code}</code></pre>
            </div>`;
        })
        .replace(/<pre><code>([\s\S]*?)<\/code><\/pre>/g,
            function(match, code) {
                return `<div class="code-block-wrapper">
                <div class="code-block-header">
                    <span class="code-lang">code</span>
                    <button class="copy-btn" onclick="copyCode(this)">复制</button>
                </div>
                <pre><code>${code}</code></pre>
            </div>`;
            });
}

// 为代码块添加复制事件处理
function attachCopyHandlers(el) {
    // 复制按钮已通过 onclick 直接绑定
}

// 复制代码到剪贴板
function copyCode(btn) {
    const wrapper = btn.closest('.code-block-wrapper');
    const codeEl = wrapper.querySelector('pre code');
    const text = codeEl.textContent;

    navigator.clipboard.writeText(text).then(() => {
        btn.textContent = '已复制 ✓';
    btn.classList.add('copied');
    setTimeout(() => {
        btn.textContent = '复制';
    btn.classList.remove('copied');
}, 2000);
}).catch(err => {
        console.error('复制失败:', err);
    btn.textContent = '复制失败';
    setTimeout(() => {
        btn.textContent = '复制';
}, 2000);
});
}

function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9);
}
