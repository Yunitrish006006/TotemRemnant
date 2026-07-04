/**
 * DeadRecall - Cloudflare Worker
 * Minecraft ↔ Discord 聊天橋接中介服務（支援多頻道）
 *
 * 環境變數（在 Cloudflare Dashboard 或 wrangler.toml 的 [vars] 設定）：
 *   MC_API_KEY            - 自訂的 API 金鑰，用於驗證來自 Minecraft 伺服器的請求
 *   DISCORD_WEBHOOK_URLS  - Discord Webhook URL 陣列 (JSON 字串)
 *                           範例: ["https://discord.com/api/webhooks/123/abc"]
 *   DISCORD_BOT_TOKEN     - Discord Bot Token（用於直接發送訊息到頻道，可選）
 */

const CORS_HEADERS = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, X-API-Key',
};

function json(data, status = 200) {
    return new Response(JSON.stringify(data), {
        status,
        headers: { 'Content-Type': 'application/json', ...CORS_HEADERS },
    });
}

function validateApiKey(request, env) {
    const key = request.headers.get('X-API-Key');
    return key && key === env.MC_API_KEY;
}

function truncateForLog(text, maxLength = 500) {
    if (!text) {
        return '';
    }
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

/**
 * 根據頻道 ID 陣列發送訊息
 * 可同時支援 Webhook 方式和 Bot Token 方式
 */
async function sendToChannels(channels, content, username, env) {
    if (!channels || channels.length === 0) {
        // 沒有指定頻道，使用預設 Webhook URLs
        return await sendToWebhooks(content, username, env);
    }

    // 若有指定頻道 ID，優先嘗試 Bot Token 方式
    if (env.DISCORD_BOT_TOKEN) {
        return await sendToChannelsWithBot(channels, content, username, env);
    } else {
        // 退而求其次用 Webhook
        return await sendToWebhooks(content, username, env);
    }
}

/**
 * 使用 Discord Bot Token 發送訊息到指定頻道
 */
async function sendToChannelsWithBot(channels, content, username, env) {
    const results = await Promise.all(
        channels.map(async channelId => {
            try {
                const res = await fetch(`https://discord.com/api/v10/channels/${channelId}/messages`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bot ${env.DISCORD_BOT_TOKEN}`,
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        content: `**${username}**: ${content}`,
                    }),
                });
                const responseText = await res.text();
                if (!res.ok) {
                    return {
                        channelId,
                        ok: false,
                        status: res.status,
                        statusText: res.statusText,
                        body: truncateForLog(responseText),
                    };
                }
                return { channelId, ok: true, status: res.status };
            } catch (error) {
                return {
                    channelId,
                    ok: false,
                    error: error instanceof Error ? error.message : String(error),
                };
            }
        })
    );

    const sent = results.filter(result => result.ok).length;
    const failed = results.length - sent;

    for (const result of results) {
        if (!result.ok) {
            console.warn(
                `[channels-bot] channel=${result.channelId} failed ` +
                `${result.status ? `status=${result.status} ` : ''}` +
                `${result.statusText ? `statusText=${result.statusText} ` : ''}` +
                `${result.error ? `error=${result.error} ` : ''}` +
                `${result.body ? `body=${result.body}` : ''}`.trim()
            );
        }
    }

    console.log(`[channels-bot] sent=${sent} failed=${failed}`);
    return { sent, failed, total: channels.length, results };
}

/**
 * 使用 Webhook URLs 發送訊息
 */
async function sendToWebhooks(content, username, env) {
    let webhookUrls = [];
    try {
        webhookUrls = JSON.parse(env.DISCORD_WEBHOOK_URLS || '[]');
    } catch {
        throw new Error('Worker misconfigured: invalid DISCORD_WEBHOOK_URLS');
    }

    if (!Array.isArray(webhookUrls) || webhookUrls.length === 0) {
        throw new Error('No Discord webhooks configured');
    }

    const results = await Promise.all(
        webhookUrls.map(async webhookUrl => {
            try {
                const res = await fetch(webhookUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        content: `**${username}**: ${content}`,
                        username: 'Minecraft Server',
                    }),
                });
                const responseText = await res.text();
                if (!res.ok) {
                    return {
                        webhookUrl,
                        ok: false,
                        status: res.status,
                        statusText: res.statusText,
                        body: truncateForLog(responseText),
                    };
                }
                return { webhookUrl, ok: true, status: res.status };
            } catch (error) {
                return {
                    webhookUrl,
                    ok: false,
                    error: error instanceof Error ? error.message : String(error),
                };
            }
        })
    );

    const sent = results.filter(result => result.ok).length;
    const failed = results.length - sent;

    for (const result of results) {
        if (!result.ok) {
            console.warn(
                `[webhooks] target=${result.webhookUrl} failed ` +
                `${result.status ? `status=${result.status} ` : ''}` +
                `${result.statusText ? `statusText=${result.statusText} ` : ''}` +
                `${result.error ? `error=${result.error} ` : ''}` +
                `${result.body ? `body=${result.body}` : ''}`.trim()
            );
        }
    }

    console.log(`[webhooks] sent=${sent} failed=${failed}`);
    return { sent, failed, total: webhookUrls.length, results };
}

export default {
    async fetch(request, env) {
        const url = new URL(request.url);

        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: CORS_HEADERS });
        }

        // ── /api/mc/chat  (Minecraft 聊天 → Discord) ──────────────────────────
        if (url.pathname === '/api/mc/chat' && request.method === 'POST') {
            if (!validateApiKey(request, env)) {
                return json({ success: false, error: 'Invalid API key' }, 401);
            }

            let body;
            try {
                body = await request.json();
            } catch {
                return json({ success: false, error: 'Invalid JSON body' }, 400);
            }

            const { username, message, channels } = body;
            if (!username || !message) {
                return json({ success: false, error: 'Missing username or message' }, 400);
            }

            try {
                const result = await sendToChannels(channels, message, username, env);
                return json({
                    success: true,
                    data: result,
                });
            } catch (error) {
                console.error('[chat] Error:', error.message);
                return json({ success: false, error: error.message }, 500);
            }
        }

        // ── /api/mc/server/status  (伺服器狀態通知 → Discord) ─────────────────
        if (url.pathname === '/api/mc/server/status' && request.method === 'POST') {
            if (!validateApiKey(request, env)) {
                return json({ success: false, error: 'Invalid API key' }, 401);
            }

            let body;
            try {
                body = await request.json();
            } catch {
                return json({ success: false, error: 'Invalid JSON body' }, 400);
            }

            const { status, players_online, players_max, version, tps, channels } = body;

            try {
                let webhookUrls = [];
                try {
                    webhookUrls = JSON.parse(env.DISCORD_WEBHOOK_URLS || '[]');
                } catch {
                    // ignore
                }

                if (webhookUrls.length > 0) {
                    const embed = {
                        title: '🖥️ 伺服器狀態更新',
                        color: status === 'online' ? 0x57F287 : 0xED4245,
                        fields: [
                            { name: '狀態',    value: status ?? 'unknown',                inline: true },
                            { name: '玩家',    value: `${players_online ?? 0}/${players_max ?? 0}`, inline: true },
                            { name: '版本',    value: version ?? 'unknown',               inline: true },
                            { name: 'TPS',     value: tps != null ? String(tps) : 'N/A',  inline: true },
                        ],
                        timestamp: new Date().toISOString(),
                    };

                    await Promise.allSettled(
                        webhookUrls.map(webhookUrl =>
                            fetch(webhookUrl, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ embeds: [embed], username: 'Minecraft Server' }),
                            })
                        )
                    );
                }

                return json({ success: true, message: 'Status received' });
            } catch (error) {
                console.error('[status] Error:', error.message);
                return json({ success: false, error: error.message }, 500);
            }
        }

        return json({ error: 'Not found' }, 404);
    },
};
