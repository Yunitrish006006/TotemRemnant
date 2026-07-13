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
const DISCORD_CONTENT_LIMIT = 1900;
const DISCORD_ERROR_BODY_LIMIT = 500;
const MAX_CHANNELS = 10;
const CHANNEL_ID_PATTERN = /^\d{17,20}$/;
const WEBHOOK_URL_PATTERN = /https:\/\/(?:canary\.|ptb\.)?discord(?:app)?\.com\/api\/webhooks\/[^\s"')]+/gi;

function json(data, status = 200) {
    return new Response(JSON.stringify(data), {
        status,
        headers: { 'Content-Type': 'application/json', ...CORS_HEADERS },
    });
}

async function validateApiKey(request, env) {
    const key = request.headers.get('X-API-Key');
    if (!key || !env.MC_API_KEY) {
        return false;
    }
    return await timingSafeEqual(key, env.MC_API_KEY);
}

async function timingSafeEqual(left, right) {
    const encoder = new TextEncoder();
    const [leftHash, rightHash] = await Promise.all([
        crypto.subtle.digest('SHA-256', encoder.encode(left)),
        crypto.subtle.digest('SHA-256', encoder.encode(right)),
    ]);
    const leftBytes = new Uint8Array(leftHash);
    const rightBytes = new Uint8Array(rightHash);
    let diff = leftBytes.length ^ rightBytes.length;
    for (let i = 0; i < leftBytes.length && i < rightBytes.length; i++) {
        diff |= leftBytes[i] ^ rightBytes[i];
    }
    return diff === 0;
}

function redactSecrets(text) {
    return String(text ?? '').replace(WEBHOOK_URL_PATTERN, '[redacted-discord-webhook]');
}

function truncateForLog(text, maxLength = 500) {
    if (!text) {
        return '';
    }
    const redacted = redactSecrets(text);
    return redacted.length > maxLength ? `${redacted.slice(0, maxLength)}...` : redacted;
}

function truncateForDiscord(text, maxLength = DISCORD_CONTENT_LIMIT) {
    const value = String(text ?? '');
    return value.length > maxLength ? `${value.slice(0, maxLength - 1)}…` : value;
}

function embedValue(value, fallback = 'unknown') {
    return truncateForDiscord(value ?? fallback, 1000);
}

function safeErrorMessage(error) {
    const message = error instanceof Error ? error.message : String(error);
    return truncateForLog(message);
}

async function readLimitedText(response, maxLength = DISCORD_ERROR_BODY_LIMIT) {
    if (!response.body) {
        return '';
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let text = '';

    try {
        while (text.length <= maxLength) {
            const { done, value } = await reader.read();
            if (done) {
                break;
            }
            text += decoder.decode(value, { stream: true });
            if (text.length > maxLength) {
                await reader.cancel();
                return truncateForLog(text, maxLength);
            }
        }
        text += decoder.decode();
        return truncateForLog(text, maxLength);
    } finally {
        reader.releaseLock();
    }
}

function normalizeChannels(channels) {
    if (!Array.isArray(channels)) {
        return [];
    }

    const normalized = [];
    for (const channel of channels) {
        const channelId = String(channel ?? '').trim();
        if (!CHANNEL_ID_PATTERN.test(channelId) || normalized.includes(channelId)) {
            continue;
        }
        normalized.push(channelId);
        if (normalized.length >= MAX_CHANNELS) {
            break;
        }
    }
    return normalized;
}

function webhookTarget(index) {
    return `webhook:${index + 1}`;
}

function formatMinecraftMessage(event, username, message) {
    const normalizedEvent = typeof event === 'string' ? event.toLowerCase() : 'chat';
    const safeUsername = truncateForDiscord(username, 80);
    const safeMessage = truncateForDiscord(message);
    let formatted;
    switch (normalizedEvent) {
        case 'player_first_join':
            formatted = `**首次加入**: ${safeUsername}`;
            break;
        case 'player_join':
            formatted = `**玩家加入**: ${safeUsername}`;
            break;
        case 'player_leave':
            formatted = `**玩家離開**: ${safeUsername}`;
            break;
        case 'player_death':
            formatted = `**死亡訊息**: ${safeMessage}`;
            break;
        case 'villager_level_up':
            formatted = `**村民升級**: ${safeMessage}`;
            break;
        case 'advancement':
            formatted = `**進度達成**: ${safeMessage}`;
            break;
        case 'admin_action':
            formatted = `**管理操作**: ${safeMessage}`;
            break;
        case 'server_health_alert':
            formatted = `**伺服器健康告警**: ${safeMessage}`;
            break;
        case 'death_backpack_created':
            formatted = `**死亡背包建立**: ${safeMessage}`;
            break;
        case 'death_backpack_recovered':
            formatted = `**死亡背包回收**: ${safeMessage}`;
            break;
        case 'space_unit_public_update':
            formatted = `**公開 Space Unit**: ${safeMessage}`;
            break;
        case 'boss_defeated':
            formatted = `**Boss 擊敗**: ${safeMessage}`;
            break;
        case 'raid_started':
            formatted = `**襲擊開始**: ${safeMessage}`;
            break;
        case 'raid_ended':
            formatted = `**襲擊結束**: ${safeMessage}`;
            break;
        case 'difficulty_changed':
            formatted = `**難度變更**: ${safeMessage}`;
            break;
        case 'gamerule_changed':
            formatted = `**Gamerule 變更**: ${safeMessage}`;
            break;
        default:
            formatted = `**${safeUsername}**: ${safeMessage}`;
            break;
    }
    return truncateForDiscord(formatted);
}

/**
 * 根據頻道 ID 陣列發送訊息
 * 可同時支援 Webhook 方式和 Bot Token 方式
 */
async function sendToChannels(channels, content, username, event, env) {
    const discordContent = formatMinecraftMessage(event, username, content);
    const targetChannels = normalizeChannels(channels);

    if (targetChannels.length === 0) {
        // 沒有指定頻道，使用預設 Webhook URLs
        return await sendToWebhooks(discordContent, env);
    }

    // 若有指定頻道 ID，優先嘗試 Bot Token 方式
    if (env.DISCORD_BOT_TOKEN) {
        return await sendToChannelsWithBot(targetChannels, discordContent, env);
    } else {
        // 退而求其次用 Webhook
        return await sendToWebhooks(discordContent, env);
    }
}

/**
 * 使用 Discord Bot Token 發送訊息到指定頻道
 */
async function sendToChannelsWithBot(channels, content, env) {
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
                        content,
                    }),
                });
                const responseText = await readLimitedText(res);
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
                    error: safeErrorMessage(error),
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
async function sendToWebhooks(content, env) {
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
        webhookUrls.map(async (webhookUrl, index) => {
            const target = webhookTarget(index);
            try {
                const res = await fetch(webhookUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        content,
                        username: 'Minecraft Server',
                    }),
                });
                const responseText = await readLimitedText(res);
                if (!res.ok) {
                    return {
                        target,
                        ok: false,
                        status: res.status,
                        statusText: res.statusText,
                        body: truncateForLog(responseText),
                    };
                }
                return { target, ok: true, status: res.status };
            } catch (error) {
                return {
                    target,
                    ok: false,
                    error: safeErrorMessage(error),
                };
            }
        })
    );

    const sent = results.filter(result => result.ok).length;
    const failed = results.length - sent;

    for (const result of results) {
        if (!result.ok) {
            console.warn(
                `[webhooks] target=${result.target} failed ` +
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

async function sendEmbedsToChannelsWithBot(channels, embeds, env) {
    const results = await Promise.all(
        channels.map(async channelId => {
            try {
                const res = await fetch(`https://discord.com/api/v10/channels/${channelId}/messages`, {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bot ${env.DISCORD_BOT_TOKEN}`,
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ embeds }),
                });
                const responseText = await readLimitedText(res);
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
                    error: safeErrorMessage(error),
                };
            }
        })
    );

    const sent = results.filter(result => result.ok).length;
    const failed = results.length - sent;
    for (const result of results) {
        if (!result.ok) {
            console.warn(
                `[status-bot] channel=${result.channelId} failed ` +
                `${result.status ? `status=${result.status} ` : ''}` +
                `${result.statusText ? `statusText=${result.statusText} ` : ''}` +
                `${result.error ? `error=${result.error} ` : ''}` +
                `${result.body ? `body=${result.body}` : ''}`.trim()
            );
        }
    }
    console.log(`[status-bot] sent=${sent} failed=${failed}`);
    return { sent, failed, total: channels.length, results };
}

async function sendEmbedsToWebhooks(embeds, env) {
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
        webhookUrls.map(async (webhookUrl, index) => {
            const target = webhookTarget(index);
            try {
                const res = await fetch(webhookUrl, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ embeds, username: 'Minecraft Server' }),
                });
                const responseText = await readLimitedText(res);
                if (!res.ok) {
                    return {
                        target,
                        ok: false,
                        status: res.status,
                        statusText: res.statusText,
                        body: truncateForLog(responseText),
                    };
                }
                return { target, ok: true, status: res.status };
            } catch (error) {
                return {
                    target,
                    ok: false,
                    error: safeErrorMessage(error),
                };
            }
        })
    );

    const sent = results.filter(result => result.ok).length;
    const failed = results.length - sent;
    for (const result of results) {
        if (!result.ok) {
            console.warn(
                `[status-webhooks] target=${result.target} failed ` +
                `${result.status ? `status=${result.status} ` : ''}` +
                `${result.statusText ? `statusText=${result.statusText} ` : ''}` +
                `${result.error ? `error=${result.error} ` : ''}` +
                `${result.body ? `body=${result.body}` : ''}`.trim()
            );
        }
    }
    console.log(`[status-webhooks] sent=${sent} failed=${failed}`);
    return { sent, failed, total: webhookUrls.length, results };
}

async function sendStatusEmbed(channels, embed, env) {
    const targetChannels = normalizeChannels(channels);
    if (targetChannels.length > 0 && env.DISCORD_BOT_TOKEN) {
        return await sendEmbedsToChannelsWithBot(targetChannels, [embed], env);
    }
    return await sendEmbedsToWebhooks([embed], env);
}

function statusColor(status) {
    const normalized = String(status ?? '').toLowerCase();
    return normalized.includes('開') || normalized.includes('online') || normalized.includes('start')
        ? 0x57F287
        : 0xED4245;
}

export default {
    async fetch(request, env) {
        const url = new URL(request.url);

        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: CORS_HEADERS });
        }

        // ── /api/mc/chat  (Minecraft 文字事件 → Discord) ───────────────────────
        if (url.pathname === '/api/mc/chat' && request.method === 'POST') {
            if (!await validateApiKey(request, env)) {
                return json({ success: false, error: 'Invalid API key' }, 401);
            }

            let body;
            try {
                body = await request.json();
            } catch {
                return json({ success: false, error: 'Invalid JSON body' }, 400);
            }

            const { event, username, message, channels } = body;
            if (typeof username !== 'string' || username.length === 0
                || typeof message !== 'string' || message.length === 0) {
                return json({ success: false, error: 'Missing username or message' }, 400);
            }

            try {
                const result = await sendToChannels(channels, message, username, event, env);
                return json({
                    success: true,
                    data: result,
                });
            } catch (error) {
                const errorMessage = safeErrorMessage(error);
                console.error('[chat] Error:', errorMessage);
                return json({ success: false, error: errorMessage }, 500);
            }
        }

        // ── /api/mc/server/status  (伺服器狀態通知 → Discord) ─────────────────
        if (url.pathname === '/api/mc/server/status' && request.method === 'POST') {
            if (!await validateApiKey(request, env)) {
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
                const embed = {
                    title: '伺服器狀態更新',
                    color: statusColor(status),
                    fields: [
                        { name: '狀態', value: embedValue(status), inline: true },
                        { name: '玩家', value: `${players_online ?? 0}/${players_max ?? 0}`, inline: true },
                        { name: '版本', value: embedValue(version), inline: true },
                        { name: 'TPS', value: embedValue(tps, 'N/A'), inline: true },
                    ],
                    timestamp: new Date().toISOString(),
                };
                const result = await sendStatusEmbed(channels, embed, env);

                return json({ success: true, data: result });
            } catch (error) {
                const errorMessage = safeErrorMessage(error);
                console.error('[status] Error:', errorMessage);
                return json({ success: false, error: errorMessage }, 500);
            }
        }

        return json({ error: 'Not found' }, 404);
    },
};
