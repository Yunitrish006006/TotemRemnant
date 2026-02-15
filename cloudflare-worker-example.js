/**
 * Cloudflare Worker - Minecraft Discord Bridge
 *
 * 環境變數設定：
 * - MC_API_KEY: Minecraft 伺服器的 API Key
 * - DISCORD_WEBHOOK_URLS: Discord Webhook URLs (JSON 陣列字串)
 *   範例: ["https://discord.com/api/webhooks/xxx/yyy"]
 */

export default {
    async fetch(request, env) {
        const url = new URL(request.url);

        // CORS headers
        const corsHeaders = {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'POST, OPTIONS',
            'Access-Control-Allow-Headers': 'Content-Type, X-API-Key',
        };

        // Handle CORS preflight
        if (request.method === 'OPTIONS') {
            return new Response(null, { headers: corsHeaders });
        }

        // ========== Minecraft Chat Endpoint ==========
        if (url.pathname === '/api/mc/chat' && request.method === 'POST') {
            try {
                // 驗證 API Key
                const apiKey = request.headers.get('X-API-Key');
                if (!apiKey || apiKey !== env.MC_API_KEY) {
                    console.error('Invalid API key:', apiKey?.substring(0, 10) + '...');
                    return new Response(JSON.stringify({
                        success: false,
                        error: 'Invalid API key'
                    }), {
                        status: 401,
                        headers: {
                            'Content-Type': 'application/json',
                            ...corsHeaders
                        }
                    });
                }

                // 解析請求
                const { username, message } = await request.json();

                if (!username || !message) {
                    return new Response(JSON.stringify({
                        success: false,
                        error: 'Missing username or message'
                    }), {
                        status: 400,
                        headers: {
                            'Content-Type': 'application/json',
                            ...corsHeaders
                        }
                    });
                }

                console.log(`收到聊天訊息 - 玩家: ${username}, 訊息: ${message}`);

                // 讀取 Discord Webhook URLs
                let webhookUrls = [];
                try {
                    webhookUrls = JSON.parse(env.DISCORD_WEBHOOK_URLS || '[]');
                } catch (e) {
                    console.error('解析 DISCORD_WEBHOOK_URLS 失敗:', e);
                    return new Response(JSON.stringify({
                        success: false,
                        error: 'Worker configuration error: Invalid DISCORD_WEBHOOK_URLS'
                    }), {
                        status: 500,
                        headers: {
                            'Content-Type': 'application/json',
                            ...corsHeaders
                        }
                    });
                }

                if (!Array.isArray(webhookUrls) || webhookUrls.length === 0) {
                    console.error('沒有設定 Discord Webhook URLs');
                    return new Response(JSON.stringify({
                        success: false,
                        error: 'No Discord webhooks configured'
                    }), {
                        status: 500,
                        headers: {
                            'Content-Type': 'application/json',
                            ...corsHeaders
                        }
                    });
                }

                console.log(`準備發送到 ${webhookUrls.length} 個 Discord Webhooks`);

                // 發送到所有 Discord Webhooks
                const results = await Promise.allSettled(
                    webhookUrls.map(async (webhookUrl, index) => {
                        try {
                            // Discord Webhook 訊息格式
                            const discordBody = {
                                content: `**${username}**: ${message}`,
                                username: "Minecraft Server",
                                // avatar_url: "https://mc-heads.net/avatar/" + username // 可選：使用玩家頭像
                            };

                            console.log(`發送到 Webhook ${index + 1}:`, webhookUrl.substring(0, 50) + '...');

                            const response = await fetch(webhookUrl, {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify(discordBody)
                            });

                            if (!response.ok) {
                                const errorText = await response.text();
                                console.error(`Webhook ${index + 1} 失敗 (${response.status}):`, errorText);
                                throw new Error(`HTTP ${response.status}: ${errorText}`);
                            }

                            console.log(`Webhook ${index + 1} 發送成功`);
                            return { success: true, index };

                        } catch (error) {
                            console.error(`Webhook ${index + 1} 錯誤:`, error.message);
                            throw error;
                        }
                    })
                );

                // 統計結果
                const sent = results.filter(r => r.status === 'fulfilled').length;
                const failed = results.filter(r => r.status === 'rejected').length;

                // 記錄失敗的詳細信息
                if (failed > 0) {
                    const failedReasons = results
                        .map((r, i) => ({ result: r, index: i }))
                        .filter(({ result }) => result.status === 'rejected')
                        .map(({ result, index }) => `Webhook ${index + 1}: ${result.reason.message}`);

                    console.error('失敗的 Webhooks:', failedReasons);
                }

                console.log(`發送完成 - 成功: ${sent}, 失敗: ${failed}`);

                return new Response(JSON.stringify({
                    success: true,
                    data: {
                        sent,
                        failed,
                        total: webhookUrls.length
                    }
                }), {
                    headers: {
                        'Content-Type': 'application/json',
                        ...corsHeaders
                    }
                });

            } catch (error) {
                console.error('處理請求時發生錯誤:', error);
                return new Response(JSON.stringify({
                    success: false,
                    error: error.message
                }), {
                    status: 500,
                    headers: {
                        'Content-Type': 'application/json',
                        ...corsHeaders
                    }
                });
            }
        }

        // ========== Server Status Endpoint ==========
        if (url.pathname === '/api/mc/server/status' && request.method === 'POST') {
            try {
                // 驗證 API Key
                const apiKey = request.headers.get('X-API-Key');
                if (!apiKey || apiKey !== env.MC_API_KEY) {
                    return new Response(JSON.stringify({
                        success: false,
                        error: 'Invalid API key'
                    }), {
                        status: 401,
                        headers: {
                            'Content-Type': 'application/json',
                            ...corsHeaders
                        }
                    });
                }

                const { status, players_online, players_max, version, tps } = await request.json();

                console.log(`伺服器狀態更新 - 狀態: ${status}, 玩家: ${players_online}/${players_max}, TPS: ${tps}`);

                // 這裡可以實作狀態更新的邏輯
                // 例如：更新 Discord 狀態頻道、發送通知等

                return new Response(JSON.stringify({
                    success: true,
                    message: 'Status received'
                }), {
                    headers: {
                        'Content-Type': 'application/json',
                        ...corsHeaders
                    }
                });

            } catch (error) {
                console.error('處理狀態請求時發生錯誤:', error);
                return new Response(JSON.stringify({
                    success: false,
                    error: error.message
                }), {
                    status: 500,
                    headers: {
                        'Content-Type': 'application/json',
                        ...corsHeaders
                    }
                });
            }
        }

        // 404
        return new Response(JSON.stringify({
            error: 'Not found'
        }), {
            status: 404,
            headers: {
                'Content-Type': 'application/json',
                ...corsHeaders
            }
        });
    }
};
