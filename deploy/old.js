var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __name = (target, value) => __defProp(target, "name", { value, configurable: true });
var __require = /* @__PURE__ */ ((x) => typeof require !== "undefined" ? require : typeof Proxy !== "undefined" ? new Proxy(x, {
    get: (a2, b) => (typeof require !== "undefined" ? require : a2)[b]
}) : x)(function(x) {
    if (typeof require !== "undefined")
        return require.apply(this, arguments);
    throw new Error('Dynamic require of "' + x + '" is not supported');
});
var __commonJS = (cb, mod) => function __require2() {
    return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};
var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
        for (let key of __getOwnPropNames(from))
            if (!__hasOwnProp.call(to, key) && key !== except)
                __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
    // If the importer is in node compatibility mode or this is not an ESM
    // file that has been converted to a CommonJS file using a Babel-
    // compatible transform (i.e. "__esModule" has not been set), then set
    // "default" to the CommonJS "module.exports" for node compatibility.
    isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
    mod
));

// node_modules/discord-interactions/dist/util.js
var require_util = __commonJS({
    "node_modules/discord-interactions/dist/util.js"(exports) {
        "use strict";
        Object.defineProperty(exports, "__esModule", { value: true });
        exports.concatUint8Arrays = exports.valueToUint8Array = exports.subtleCrypto = void 0;
        function getSubtleCrypto() {
            if (typeof window !== "undefined" && window.crypto) {
                return window.crypto.subtle;
            }
            if (typeof globalThis !== "undefined" && globalThis.crypto) {
                return globalThis.crypto.subtle;
            }
            if (typeof crypto !== "undefined") {
                return crypto.subtle;
            }
            if (typeof __require === "function") {
                const cryptoPackage = "node:crypto";
                const crypto2 = __require(cryptoPackage);
                return crypto2.webcrypto.subtle;
            }
            throw new Error("No Web Crypto API implementation found");
        }
        __name(getSubtleCrypto, "getSubtleCrypto");
        exports.subtleCrypto = getSubtleCrypto();
        function valueToUint8Array(value, format) {
            if (value == null) {
                return new Uint8Array();
            }
            if (typeof value === "string") {
                if (format === "hex") {
                    const matches = value.match(/.{1,2}/g);
                    if (matches == null) {
                        throw new Error("Value is not a valid hex string");
                    }
                    const hexVal = matches.map((byte) => Number.parseInt(byte, 16));
                    return new Uint8Array(hexVal);
                }
                return new TextEncoder().encode(value);
            }
            try {
                if (Buffer.isBuffer(value)) {
                    return new Uint8Array(value);
                }
            } catch (_ex) {
            }
            if (value instanceof ArrayBuffer) {
                return new Uint8Array(value);
            }
            if (value instanceof Uint8Array) {
                return value;
            }
            throw new Error("Unrecognized value type, must be one of: string, Buffer, ArrayBuffer, Uint8Array");
        }
        __name(valueToUint8Array, "valueToUint8Array");
        exports.valueToUint8Array = valueToUint8Array;
        function concatUint8Arrays(arr1, arr2) {
            const merged = new Uint8Array(arr1.length + arr2.length);
            merged.set(arr1);
            merged.set(arr2, arr1.length);
            return merged;
        }
        __name(concatUint8Arrays, "concatUint8Arrays");
        exports.concatUint8Arrays = concatUint8Arrays;
    }
});

// node_modules/discord-interactions/dist/webhooks.js
var require_webhooks = __commonJS({
    "node_modules/discord-interactions/dist/webhooks.js"(exports) {
        "use strict";
        Object.defineProperty(exports, "__esModule", { value: true });
        exports.WebhookEventType = exports.WebhookType = void 0;
        var WebhookType;
        (function(WebhookType2) {
            WebhookType2[WebhookType2["PING"] = 0] = "PING";
            WebhookType2[WebhookType2["EVENT"] = 1] = "EVENT";
        })(WebhookType || (exports.WebhookType = WebhookType = {}));
        var WebhookEventType;
        (function(WebhookEventType2) {
            WebhookEventType2["APPLICATION_AUTHORIZED"] = "APPLICATION_AUTHORIZED";
            WebhookEventType2["APPLICATION_DEAUTHORIZED"] = "APPLICATION_DEAUTHORIZED";
            WebhookEventType2["ENTITLEMENT_CREATE"] = "ENTITLEMENT_CREATE";
            WebhookEventType2["QUEST_USER_ENROLLMENT"] = "QUEST_USER_ENROLLMENT";
            WebhookEventType2["LOBBY_MESSAGE_CREATE"] = "LOBBY_MESSAGE_CREATE";
            WebhookEventType2["LOBBY_MESSAGE_UPDATE"] = "LOBBY_MESSAGE_UPDATE";
            WebhookEventType2["LOBBY_MESSAGE_DELETE"] = "LOBBY_MESSAGE_DELETE";
            WebhookEventType2["GAME_DIRECT_MESSAGE_CREATE"] = "GAME_DIRECT_MESSAGE_CREATE";
            WebhookEventType2["GAME_DIRECT_MESSAGE_UPDATE"] = "GAME_DIRECT_MESSAGE_UPDATE";
            WebhookEventType2["GAME_DIRECT_MESSAGE_DELETE"] = "GAME_DIRECT_MESSAGE_DELETE";
        })(WebhookEventType || (exports.WebhookEventType = WebhookEventType = {}));
    }
});

// node_modules/discord-interactions/dist/components.js
var require_components = __commonJS({
    "node_modules/discord-interactions/dist/components.js"(exports) {
        "use strict";
        Object.defineProperty(exports, "__esModule", { value: true });
        exports.SeparatorSpacingTypes = exports.TextStyleTypes = exports.ChannelTypes = exports.ButtonStyleTypes = exports.MessageComponentTypes = void 0;
        var MessageComponentTypes;
        (function(MessageComponentTypes2) {
            MessageComponentTypes2[MessageComponentTypes2["ACTION_ROW"] = 1] = "ACTION_ROW";
            MessageComponentTypes2[MessageComponentTypes2["BUTTON"] = 2] = "BUTTON";
            MessageComponentTypes2[MessageComponentTypes2["STRING_SELECT"] = 3] = "STRING_SELECT";
            MessageComponentTypes2[MessageComponentTypes2["INPUT_TEXT"] = 4] = "INPUT_TEXT";
            MessageComponentTypes2[MessageComponentTypes2["USER_SELECT"] = 5] = "USER_SELECT";
            MessageComponentTypes2[MessageComponentTypes2["ROLE_SELECT"] = 6] = "ROLE_SELECT";
            MessageComponentTypes2[MessageComponentTypes2["MENTIONABLE_SELECT"] = 7] = "MENTIONABLE_SELECT";
            MessageComponentTypes2[MessageComponentTypes2["CHANNEL_SELECT"] = 8] = "CHANNEL_SELECT";
            MessageComponentTypes2[MessageComponentTypes2["SECTION"] = 9] = "SECTION";
            MessageComponentTypes2[MessageComponentTypes2["TEXT_DISPLAY"] = 10] = "TEXT_DISPLAY";
            MessageComponentTypes2[MessageComponentTypes2["THUMBNAIL"] = 11] = "THUMBNAIL";
            MessageComponentTypes2[MessageComponentTypes2["MEDIA_GALLERY"] = 12] = "MEDIA_GALLERY";
            MessageComponentTypes2[MessageComponentTypes2["FILE"] = 13] = "FILE";
            MessageComponentTypes2[MessageComponentTypes2["SEPARATOR"] = 14] = "SEPARATOR";
            MessageComponentTypes2[MessageComponentTypes2["CONTAINER"] = 17] = "CONTAINER";
            MessageComponentTypes2[MessageComponentTypes2["LABEL"] = 18] = "LABEL";
        })(MessageComponentTypes || (exports.MessageComponentTypes = MessageComponentTypes = {}));
        var ButtonStyleTypes;
        (function(ButtonStyleTypes2) {
            ButtonStyleTypes2[ButtonStyleTypes2["PRIMARY"] = 1] = "PRIMARY";
            ButtonStyleTypes2[ButtonStyleTypes2["SECONDARY"] = 2] = "SECONDARY";
            ButtonStyleTypes2[ButtonStyleTypes2["SUCCESS"] = 3] = "SUCCESS";
            ButtonStyleTypes2[ButtonStyleTypes2["DANGER"] = 4] = "DANGER";
            ButtonStyleTypes2[ButtonStyleTypes2["LINK"] = 5] = "LINK";
            ButtonStyleTypes2[ButtonStyleTypes2["PREMIUM"] = 6] = "PREMIUM";
        })(ButtonStyleTypes || (exports.ButtonStyleTypes = ButtonStyleTypes = {}));
        var ChannelTypes;
        (function(ChannelTypes2) {
            ChannelTypes2[ChannelTypes2["GUILD_TEXT"] = 0] = "GUILD_TEXT";
            ChannelTypes2[ChannelTypes2["DM"] = 1] = "DM";
            ChannelTypes2[ChannelTypes2["GUILD_VOICE"] = 2] = "GUILD_VOICE";
            ChannelTypes2[ChannelTypes2["GROUP_DM"] = 3] = "GROUP_DM";
            ChannelTypes2[ChannelTypes2["GUILD_CATEGORY"] = 4] = "GUILD_CATEGORY";
            ChannelTypes2[ChannelTypes2["GUILD_ANNOUNCEMENT"] = 5] = "GUILD_ANNOUNCEMENT";
            ChannelTypes2[ChannelTypes2["GUILD_STORE"] = 6] = "GUILD_STORE";
            ChannelTypes2[ChannelTypes2["ANNOUNCEMENT_THREAD"] = 10] = "ANNOUNCEMENT_THREAD";
            ChannelTypes2[ChannelTypes2["PUBLIC_THREAD"] = 11] = "PUBLIC_THREAD";
            ChannelTypes2[ChannelTypes2["PRIVATE_THREAD"] = 12] = "PRIVATE_THREAD";
            ChannelTypes2[ChannelTypes2["GUILD_STAGE_VOICE"] = 13] = "GUILD_STAGE_VOICE";
            ChannelTypes2[ChannelTypes2["GUILD_DIRECTORY"] = 14] = "GUILD_DIRECTORY";
            ChannelTypes2[ChannelTypes2["GUILD_FORUM"] = 15] = "GUILD_FORUM";
            ChannelTypes2[ChannelTypes2["GUILD_MEDIA"] = 16] = "GUILD_MEDIA";
        })(ChannelTypes || (exports.ChannelTypes = ChannelTypes = {}));
        var TextStyleTypes;
        (function(TextStyleTypes2) {
            TextStyleTypes2[TextStyleTypes2["SHORT"] = 1] = "SHORT";
            TextStyleTypes2[TextStyleTypes2["PARAGRAPH"] = 2] = "PARAGRAPH";
        })(TextStyleTypes || (exports.TextStyleTypes = TextStyleTypes = {}));
        var SeparatorSpacingTypes;
        (function(SeparatorSpacingTypes2) {
            SeparatorSpacingTypes2[SeparatorSpacingTypes2["SMALL"] = 1] = "SMALL";
            SeparatorSpacingTypes2[SeparatorSpacingTypes2["LARGE"] = 2] = "LARGE";
        })(SeparatorSpacingTypes || (exports.SeparatorSpacingTypes = SeparatorSpacingTypes = {}));
    }
});

// node_modules/discord-interactions/dist/index.js
var require_dist = __commonJS({
    "node_modules/discord-interactions/dist/index.js"(exports) {
        "use strict";
        var __createBinding = exports && exports.__createBinding || (Object.create ? function(o2, m, k, k2) {
            if (k2 === void 0)
                k2 = k;
            var desc = Object.getOwnPropertyDescriptor(m, k);
            if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
                desc = { enumerable: true, get: function() {
                        return m[k];
                    } };
            }
            Object.defineProperty(o2, k2, desc);
        } : function(o2, m, k, k2) {
            if (k2 === void 0)
                k2 = k;
            o2[k2] = m[k];
        });
        var __exportStar = exports && exports.__exportStar || function(m, exports2) {
            for (var p2 in m)
                if (p2 !== "default" && !Object.prototype.hasOwnProperty.call(exports2, p2))
                    __createBinding(exports2, m, p2);
        };
        var __awaiter = exports && exports.__awaiter || function(thisArg, _arguments, P, generator) {
            function adopt(value) {
                return value instanceof P ? value : new P(function(resolve) {
                    resolve(value);
                });
            }
            __name(adopt, "adopt");
            return new (P || (P = Promise))(function(resolve, reject) {
                function fulfilled(value) {
                    try {
                        step(generator.next(value));
                    } catch (e) {
                        reject(e);
                    }
                }
                __name(fulfilled, "fulfilled");
                function rejected(value) {
                    try {
                        step(generator["throw"](value));
                    } catch (e) {
                        reject(e);
                    }
                }
                __name(rejected, "rejected");
                function step(result) {
                    result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected);
                }
                __name(step, "step");
                step((generator = generator.apply(thisArg, _arguments || [])).next());
            });
        };
        Object.defineProperty(exports, "__esModule", { value: true });
        exports.verifyWebhookEventMiddleware = exports.verifyKeyMiddleware = exports.verifyKey = exports.InteractionResponseFlags = exports.InteractionResponseType = exports.InteractionType = void 0;
        var util_1 = require_util();
        var webhooks_1 = require_webhooks();
        var InteractionType2;
        (function(InteractionType3) {
            InteractionType3[InteractionType3["PING"] = 1] = "PING";
            InteractionType3[InteractionType3["APPLICATION_COMMAND"] = 2] = "APPLICATION_COMMAND";
            InteractionType3[InteractionType3["MESSAGE_COMPONENT"] = 3] = "MESSAGE_COMPONENT";
            InteractionType3[InteractionType3["APPLICATION_COMMAND_AUTOCOMPLETE"] = 4] = "APPLICATION_COMMAND_AUTOCOMPLETE";
            InteractionType3[InteractionType3["MODAL_SUBMIT"] = 5] = "MODAL_SUBMIT";
        })(InteractionType2 || (exports.InteractionType = InteractionType2 = {}));
        var InteractionResponseType2;
        (function(InteractionResponseType3) {
            InteractionResponseType3[InteractionResponseType3["PONG"] = 1] = "PONG";
            InteractionResponseType3[InteractionResponseType3["CHANNEL_MESSAGE_WITH_SOURCE"] = 4] = "CHANNEL_MESSAGE_WITH_SOURCE";
            InteractionResponseType3[InteractionResponseType3["DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE"] = 5] = "DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE";
            InteractionResponseType3[InteractionResponseType3["DEFERRED_UPDATE_MESSAGE"] = 6] = "DEFERRED_UPDATE_MESSAGE";
            InteractionResponseType3[InteractionResponseType3["UPDATE_MESSAGE"] = 7] = "UPDATE_MESSAGE";
            InteractionResponseType3[InteractionResponseType3["APPLICATION_COMMAND_AUTOCOMPLETE_RESULT"] = 8] = "APPLICATION_COMMAND_AUTOCOMPLETE_RESULT";
            InteractionResponseType3[InteractionResponseType3["MODAL"] = 9] = "MODAL";
            InteractionResponseType3[InteractionResponseType3["PREMIUM_REQUIRED"] = 10] = "PREMIUM_REQUIRED";
            InteractionResponseType3[InteractionResponseType3["LAUNCH_ACTIVITY"] = 12] = "LAUNCH_ACTIVITY";
        })(InteractionResponseType2 || (exports.InteractionResponseType = InteractionResponseType2 = {}));
        var InteractionResponseFlags2;
        (function(InteractionResponseFlags3) {
            InteractionResponseFlags3[InteractionResponseFlags3["EPHEMERAL"] = 64] = "EPHEMERAL";
            InteractionResponseFlags3[InteractionResponseFlags3["IS_COMPONENTS_V2"] = 32768] = "IS_COMPONENTS_V2";
        })(InteractionResponseFlags2 || (exports.InteractionResponseFlags = InteractionResponseFlags2 = {}));
        function verifyKey2(rawBody, signature, timestamp, clientPublicKey) {
            return __awaiter(this, void 0, void 0, function* () {
                try {
                    const timestampData = (0, util_1.valueToUint8Array)(timestamp);
                    const bodyData = (0, util_1.valueToUint8Array)(rawBody);
                    const message = (0, util_1.concatUint8Arrays)(timestampData, bodyData);
                    const publicKey = typeof clientPublicKey === "string" ? yield util_1.subtleCrypto.importKey("raw", (0, util_1.valueToUint8Array)(clientPublicKey, "hex"), {
                        name: "ed25519",
                        namedCurve: "ed25519"
                    }, false, ["verify"]) : clientPublicKey;
                    const isValid = yield util_1.subtleCrypto.verify({
                        name: "ed25519"
                    }, publicKey, (0, util_1.valueToUint8Array)(signature, "hex"), message);
                    return isValid;
                } catch (_ex) {
                    return false;
                }
            });
        }
        __name(verifyKey2, "verifyKey");
        exports.verifyKey = verifyKey2;
        function verifyKeyMiddleware(clientPublicKey) {
            if (!clientPublicKey) {
                throw new Error("You must specify a Discord client public key");
            }
            return (req, res, next) => __awaiter(this, void 0, void 0, function* () {
                const timestamp = req.header("X-Signature-Timestamp") || "";
                const signature = req.header("X-Signature-Ed25519") || "";
                if (!timestamp || !signature) {
                    res.statusCode = 401;
                    res.end("[discord-interactions] Invalid signature");
                    return;
                }
                function onBodyComplete(rawBody) {
                    return __awaiter(this, void 0, void 0, function* () {
                        const isValid = yield verifyKey2(rawBody, signature, timestamp, clientPublicKey);
                        if (!isValid) {
                            res.statusCode = 401;
                            res.end("[discord-interactions] Invalid signature");
                            return;
                        }
                        const body = JSON.parse(rawBody.toString("utf-8")) || {};
                        if (body.type === InteractionType2.PING) {
                            res.setHeader("Content-Type", "application/json");
                            res.end(JSON.stringify({
                                type: InteractionResponseType2.PONG
                            }));
                            return;
                        }
                        req.body = body;
                        next();
                    });
                }
                __name(onBodyComplete, "onBodyComplete");
                if (req.body) {
                    if (Buffer.isBuffer(req.body)) {
                        yield onBodyComplete(req.body);
                    } else if (typeof req.body === "string") {
                        yield onBodyComplete(Buffer.from(req.body, "utf-8"));
                    } else {
                        console.warn("[discord-interactions]: req.body was tampered with, probably by some other middleware. We recommend disabling middleware for interaction routes so that req.body is a raw buffer.");
                        yield onBodyComplete(Buffer.from(JSON.stringify(req.body), "utf-8"));
                    }
                } else {
                    const chunks = [];
                    req.on("data", (chunk) => {
                        chunks.push(chunk);
                    });
                    req.on("end", () => __awaiter(this, void 0, void 0, function* () {
                        const rawBody = Buffer.concat(chunks);
                        yield onBodyComplete(rawBody);
                    }));
                }
            });
        }
        __name(verifyKeyMiddleware, "verifyKeyMiddleware");
        exports.verifyKeyMiddleware = verifyKeyMiddleware;
        function verifyWebhookEventMiddleware(clientPublicKey) {
            if (!clientPublicKey) {
                throw new Error("You must specify a Discord client public key");
            }
            return (req, res, next) => __awaiter(this, void 0, void 0, function* () {
                const timestamp = req.header("X-Signature-Timestamp") || "";
                const signature = req.header("X-Signature-Ed25519") || "";
                if (!timestamp || !signature) {
                    res.statusCode = 401;
                    res.end("[discord-interactions] Invalid signature");
                    return;
                }
                function onBodyComplete(rawBody) {
                    return __awaiter(this, void 0, void 0, function* () {
                        const isValid = yield verifyKey2(rawBody, signature, timestamp, clientPublicKey);
                        if (!isValid) {
                            res.statusCode = 401;
                            res.end("[discord-interactions] Invalid signature");
                            return;
                        }
                        const body = JSON.parse(rawBody.toString("utf-8")) || {};
                        if (body.type === webhooks_1.WebhookType.PING) {
                            res.statusCode = 204;
                            res.end();
                            return;
                        }
                        req.body = body;
                        res.statusCode = 204;
                        res.end();
                        next();
                    });
                }
                __name(onBodyComplete, "onBodyComplete");
                if (req.body) {
                    if (Buffer.isBuffer(req.body)) {
                        yield onBodyComplete(req.body);
                    } else if (typeof req.body === "string") {
                        yield onBodyComplete(Buffer.from(req.body, "utf-8"));
                    } else {
                        console.warn("[discord-interactions]: req.body was tampered with, probably by some other middleware. We recommend disabling middleware for webhook event routes so that req.body is a raw buffer.");
                        yield onBodyComplete(Buffer.from(JSON.stringify(req.body), "utf-8"));
                    }
                } else {
                    const chunks = [];
                    req.on("data", (chunk) => {
                        chunks.push(chunk);
                    });
                    req.on("end", () => __awaiter(this, void 0, void 0, function* () {
                        const rawBody = Buffer.concat(chunks);
                        yield onBodyComplete(rawBody);
                    }));
                }
            });
        }
        __name(verifyWebhookEventMiddleware, "verifyWebhookEventMiddleware");
        exports.verifyWebhookEventMiddleware = verifyWebhookEventMiddleware;
        __exportStar(require_components(), exports);
        __exportStar(require_webhooks(), exports);
    }
});

// node_modules/itty-router/index.mjs
var t = /* @__PURE__ */ __name(({ base: e = "", routes: t2 = [], ...r2 } = {}) => ({ __proto__: new Proxy({}, { get: (r3, o2, a2, s2) => (r4, ...c2) => t2.push([o2.toUpperCase?.(), RegExp(`^${(s2 = (e + r4).replace(/\/+(\/|$)/g, "$1")).replace(/(\/?\.?):(\w+)\+/g, "($1(?<$2>*))").replace(/(\/?\.?):(\w+)/g, "($1(?<$2>[^$1/]+?))").replace(/\./g, "\\.").replace(/(\/?)\*/g, "($1.*)?")}/*$`), c2, s2]) && a2 }), routes: t2, ...r2, async fetch(e2, ...o2) {
        let a2, s2, c2 = new URL(e2.url), n2 = e2.query = { __proto__: null };
        for (let [e3, t3] of c2.searchParams)
            n2[e3] = n2[e3] ? [].concat(n2[e3], t3) : t3;
        e:
            try {
                for (let t3 of r2.before || [])
                    if (null != (a2 = await t3(e2.proxy ?? e2, ...o2)))
                        break e;
                t:
                    for (let [r3, n3, l, i] of t2)
                        if ((r3 == e2.method || "ALL" == r3) && (s2 = c2.pathname.match(n3))) {
                            e2.params = s2.groups || {}, e2.route = i;
                            for (let t3 of l)
                                if (null != (a2 = await t3(e2.proxy ?? e2, ...o2)))
                                    break t;
                        }
            } catch (t3) {
                if (!r2.catch)
                    throw t3;
                a2 = await r2.catch(t3, e2.proxy ?? e2, ...o2);
            }
        try {
            for (let t3 of r2.finally || [])
                a2 = await t3(a2, e2.proxy ?? e2, ...o2) ?? a2;
        } catch (t3) {
            if (!r2.catch)
                throw t3;
            a2 = await r2.catch(t3, e2.proxy ?? e2, ...o2);
        }
        return a2;
    } }), "t");
var r = /* @__PURE__ */ __name((e = "text/plain; charset=utf-8", t2) => (r2, o2 = {}) => {
    if (void 0 === r2 || r2 instanceof Response)
        return r2;
    const a2 = new Response(t2?.(r2) ?? r2, o2.url ? void 0 : o2);
    return a2.headers.set("content-type", e), a2;
}, "r");
var o = r("application/json; charset=utf-8", JSON.stringify);
var a = /* @__PURE__ */ __name((e) => ({ 400: "Bad Request", 401: "Unauthorized", 403: "Forbidden", 404: "Not Found", 500: "Internal Server Error" })[e] || "Unknown Error", "a");
var s = /* @__PURE__ */ __name((e = 500, t2) => {
    if (e instanceof Error) {
        const { message: r2, ...o2 } = e;
        e = e.status || 500, t2 = { error: r2 || a(e), ...o2 };
    }
    return t2 = { status: e, ..."object" == typeof t2 ? t2 : { error: t2 || a(e) } }, o(t2, { status: e });
}, "s");
var c = /* @__PURE__ */ __name((e) => {
    e.proxy = new Proxy(e.proxy ?? e, { get: (t2, r2) => t2[r2]?.bind?.(e) ?? t2[r2] ?? t2?.params?.[r2] });
}, "c");
var n = /* @__PURE__ */ __name(({ format: e = o, missing: r2 = /* @__PURE__ */ __name(() => s(404), "r"), finally: a2 = [], before: n2 = [], ...l } = {}) => t({ before: [c, ...n2], catch: s, finally: [(e2, ...t2) => e2 ?? r2(...t2), e, ...a2], ...l }), "n");
var p = r("text/plain; charset=utf-8", String);
var f = r("text/html");
var u = r("image/jpeg");
var h = r("image/png");
var g = r("image/webp");

// src/middleware/verify.js
var import_discord_interactions = __toESM(require_dist(), 1);
async function verifyDiscordRequest(request, env) {
    const signature = request.headers.get("x-signature-ed25519");
    const timestamp = request.headers.get("x-signature-timestamp");
    const body = await request.text();
    const isValid = signature && timestamp && await (0, import_discord_interactions.verifyKey)(body, signature, timestamp, env.DISCORD_PUBLIC_KEY.trim());
    if (!isValid) {
        return { isValid: false };
    }
    return { interaction: JSON.parse(body), isValid: true };
}
__name(verifyDiscordRequest, "verifyDiscordRequest");

// src/middleware/auth.js
function authenticateMinecraft(request, env) {
    const apiKey = request.headers.get("X-API-Key");
    if (!apiKey || apiKey !== env.MINECRAFT_API_KEY.trim()) {
        return Response.json(
            { success: false, error: "Unauthorized" },
            { status: 401 }
        );
    }
    return null;
}
__name(authenticateMinecraft, "authenticateMinecraft");

// src/handlers/discord.js
var import_discord_interactions2 = __toESM(require_dist(), 1);

// src/commands.js
var CommandOptionType = {
    STRING: 3,
    INTEGER: 4,
    BOOLEAN: 5,
    USER: 6,
    ROLE: 8
};
var CommandNames = {
    TEST: "test",
    MC: "mc",
    STATUS: "status",
    PLAYERS: "players",
    BIND: "bind",
    SETCHANNEL: "setchannel",
    REMOVECHANNEL: "removechannel",
    TAG: "tag"
};
var COMMANDS = [
    {
        name: CommandNames.TEST,
        description: "\u6E2C\u8A66\u6A5F\u5668\u4EBA\u662F\u5426\u6B63\u5E38\u904B\u4F5C"
    },
    {
        name: CommandNames.MC,
        description: "\u50B3\u9001\u8A0A\u606F\u5230 Minecraft \u4F3A\u670D\u5668",
        options: [
            {
                name: "message",
                description: "\u8981\u50B3\u9001\u7684\u8A0A\u606F\u5167\u5BB9",
                type: CommandOptionType.STRING,
                required: true
            }
        ]
    },
    {
        name: CommandNames.STATUS,
        description: "\u67E5\u8A62 Minecraft \u4F3A\u670D\u5668\u72C0\u614B"
    },
    {
        name: CommandNames.PLAYERS,
        description: "\u67E5\u8A62\u7DDA\u4E0A\u73A9\u5BB6\u5217\u8868"
    },
    {
        name: CommandNames.BIND,
        description: "\u7D81\u5B9A Discord \u5E33\u865F\u8207 Minecraft \u5E33\u865F",
        options: [
            {
                name: "mc_username",
                description: "Minecraft \u4F7F\u7528\u8005\u540D\u7A31",
                type: CommandOptionType.STRING,
                required: true
            }
        ]
    },
    {
        name: CommandNames.TAG,
        description: "\u5EFA\u7ACB\u8EAB\u5206\u7D44\u9078\u64C7\u6309\u9215\uFF0C\u4F7F\u7528\u8005\u9EDE\u64CA\u5373\u53EF\u7372\u5F97/\u79FB\u9664\u8EAB\u5206\u7D44",
        default_member_permissions: "268435456",
        // MANAGE_ROLES
        options: [
            {
                name: "role1",
                description: "\u8EAB\u5206\u7D44 1",
                type: CommandOptionType.ROLE,
                required: true
            },
            {
                name: "role2",
                description: "\u8EAB\u5206\u7D44 2",
                type: CommandOptionType.ROLE,
                required: false
            },
            {
                name: "role3",
                description: "\u8EAB\u5206\u7D44 3",
                type: CommandOptionType.ROLE,
                required: false
            },
            {
                name: "role4",
                description: "\u8EAB\u5206\u7D44 4",
                type: CommandOptionType.ROLE,
                required: false
            },
            {
                name: "role5",
                description: "\u8EAB\u5206\u7D44 5",
                type: CommandOptionType.ROLE,
                required: false
            },
            {
                name: "title",
                description: "\u81EA\u8A02\u6A19\u984C\uFF08\u9810\u8A2D\uFF1A\u9078\u64C7\u4F60\u7684\u8EAB\u5206\u7D44\uFF09",
                type: CommandOptionType.STRING,
                required: false
            }
        ]
    },
    {
        name: CommandNames.SETCHANNEL,
        description: "\u5C07\u76EE\u524D\u983B\u9053\u8A2D\u70BA Minecraft \u804A\u5929\u540C\u6B65\u983B\u9053",
        default_member_permissions: "32"
        // MANAGE_SERVER
    },
    {
        name: CommandNames.REMOVECHANNEL,
        description: "\u79FB\u9664\u76EE\u524D\u983B\u9053\u7684 Minecraft \u804A\u5929\u540C\u6B65",
        default_member_permissions: "32"
        // MANAGE_SERVER
    }
];

// src/handlers/discord.js
async function handleDiscordInteraction(interaction, env) {
    const { type, data } = interaction;
    if (type === import_discord_interactions2.InteractionType.PING) {
        return Response.json({ type: import_discord_interactions2.InteractionResponseType.PONG });
    }
    if (type === import_discord_interactions2.InteractionType.APPLICATION_COMMAND) {
        return handleSlashCommand(interaction, env);
    }
    if (type === import_discord_interactions2.InteractionType.MESSAGE_COMPONENT) {
        return handleMessageComponent(interaction, env);
    }
    return Response.json(
        { error: "Unknown interaction type" },
        { status: 400 }
    );
}
__name(handleDiscordInteraction, "handleDiscordInteraction");
async function handleSlashCommand(interaction, env) {
    const { data, member } = interaction;
    const commandName = data.name;
    switch (commandName) {
        case CommandNames.TEST:
            return handleTestCommand(interaction, env);
        case CommandNames.MC:
            return handleMcCommand(interaction, env);
        case CommandNames.STATUS:
            return handleStatusCommand(interaction, env);
        case CommandNames.PLAYERS:
            return handlePlayersCommand(interaction, env);
        case CommandNames.BIND:
            return handleBindCommand(interaction, env);
        case CommandNames.SETCHANNEL:
            return handleSetChannelCommand(interaction, env);
        case CommandNames.REMOVECHANNEL:
            return handleRemoveChannelCommand(interaction, env);
        case CommandNames.TAG:
            return handleTagCommand(interaction, env);
        default:
            return Response.json({
                type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                data: { content: "\u274C \u672A\u77E5\u7684\u6307\u4EE4", flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL }
            });
    }
}
__name(handleSlashCommand, "handleSlashCommand");
async function handleTestCommand(interaction, env) {
    const now = /* @__PURE__ */ new Date();
    let dbStatus = "\u{1F534} \u5931\u6557";
    let dbLatency = "N/A";
    try {
        const dbStart = Date.now();
        await env.DB.prepare("SELECT 1").first();
        dbLatency = `${Date.now() - dbStart}ms`;
        dbStatus = "\u{1F7E2} \u6B63\u5E38";
    } catch (err) {
        console.error("DB test failed:", err);
    }
    return Response.json({
        type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: {
            embeds: [
                {
                    title: "\u{1F916} Bot \u72C0\u614B\u6E2C\u8A66",
                    color: 65280,
                    fields: [
                        { name: "\u72C0\u614B", value: "\u{1F7E2} \u7DDA\u4E0A", inline: true },
                        { name: "\u5EF6\u9072", value: `${Date.now() - now.getTime()}ms`, inline: true },
                        { name: "D1 \u8CC7\u6599\u5EAB", value: `${dbStatus} (${dbLatency})`, inline: true },
                        { name: "\u904B\u884C\u74B0\u5883", value: "Cloudflare Workers", inline: true },
                        { name: "\u6642\u9593", value: now.toISOString(), inline: false }
                    ]
                }
            ],
            flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
        }
    });
}
__name(handleTestCommand, "handleTestCommand");
async function handleMcCommand(interaction, env) {
    const message = getOptionValue(interaction.data.options, "message");
    const username = interaction.member?.user?.global_name || interaction.member?.user?.username || interaction.user?.username || "Unknown";
    try {
        await env.DB.prepare(
            "INSERT INTO messages (source, username, content) VALUES (?, ?, ?)"
        ).bind("discord", username, message).run();
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: `\u{1F4E8} **${username}**: ${message}
*\uFF08\u5DF2\u50B3\u9001\u81F3 Minecraft\uFF09*`
            }
        });
    } catch (err) {
        console.error("Failed to save message:", err);
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u50B3\u9001\u5931\u6557\uFF0C\u8ACB\u7A0D\u5F8C\u518D\u8A66",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(handleMcCommand, "handleMcCommand");
async function handleStatusCommand(interaction, env) {
    try {
        const settings = await env.DB.prepare(
            "SELECT key, value FROM server_settings WHERE key IN ('server_status', 'server_tps', 'server_players_online', 'server_players_max', 'server_version')"
        ).all();
        const config = {};
        for (const row of settings.results) {
            config[row.key] = row.value;
        }
        const status = config.server_status || "\u672A\u77E5";
        const tps = config.server_tps || "N/A";
        const online = config.server_players_online || "0";
        const max = config.server_players_max || "0";
        const version = config.server_version || "\u672A\u77E5";
        const isOnline = status === "online";
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                embeds: [
                    {
                        title: "\u{1F5A5}\uFE0F Minecraft \u4F3A\u670D\u5668\u72C0\u614B",
                        color: isOnline ? 65280 : 16711680,
                        fields: [
                            { name: "\u72C0\u614B", value: isOnline ? "\u{1F7E2} \u7DDA\u4E0A" : "\u{1F534} \u96E2\u7DDA", inline: true },
                            { name: "\u7248\u672C", value: version, inline: true },
                            { name: "\u73A9\u5BB6", value: `${online} / ${max}`, inline: true },
                            { name: "TPS", value: tps, inline: true }
                        ],
                        timestamp: (/* @__PURE__ */ new Date()).toISOString()
                    }
                ],
                components: [
                    {
                        type: 1,
                        // Action Row
                        components: [
                            {
                                type: 2,
                                // Button
                                style: 2,
                                // Secondary
                                label: "\u{1F504} \u91CD\u65B0\u6574\u7406",
                                custom_id: "status_refresh"
                            }
                        ]
                    }
                ]
            }
        });
    } catch (err) {
        console.error("Failed to fetch status:", err);
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u7121\u6CD5\u53D6\u5F97\u4F3A\u670D\u5668\u72C0\u614B",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(handleStatusCommand, "handleStatusCommand");
async function handlePlayersCommand(interaction, env) {
    return buildPlayersResponse(env, 0);
}
__name(handlePlayersCommand, "handlePlayersCommand");
async function handleBindCommand(interaction, env) {
    const mcUsername = getOptionValue(interaction.data.options, "mc_username");
    const discordId = interaction.member?.user?.id || interaction.user?.id;
    const discordName = interaction.member?.user?.global_name || interaction.member?.user?.username || interaction.user?.username;
    try {
        const existing = await env.DB.prepare(
            "SELECT * FROM player_bindings WHERE discord_id = ?"
        ).bind(discordId).first();
        if (existing && existing.mc_uuid) {
            return Response.json({
                type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                data: {
                    content: `\u26A0\uFE0F \u4F60\u5DF2\u7D93\u7D81\u5B9A\u4E86 Minecraft \u5E33\u865F **${existing.mc_name}**
\u82E5\u8981\u91CD\u65B0\u7D81\u5B9A\uFF0C\u8ACB\u5148\u89E3\u9664\u7D81\u5B9A\u3002`,
                    flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
                }
            });
        }
        const bindCode = generateBindCode();
        await env.DB.prepare(
            `INSERT INTO player_bindings (discord_id, discord_name, mc_name, bind_code, bind_code_at)
       VALUES (?, ?, ?, ?, datetime('now'))
       ON CONFLICT (discord_id) DO UPDATE SET
         discord_name = excluded.discord_name,
         mc_name = excluded.mc_name,
         bind_code = excluded.bind_code,
         bind_code_at = excluded.bind_code_at`
        ).bind(discordId, discordName, mcUsername, bindCode).run();
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: `\u{1F517} \u7D81\u5B9A\u6D41\u7A0B\u5DF2\u555F\u52D5\uFF01

\u8ACB\u5728 Minecraft \u4E2D\u57F7\u884C\u4EE5\u4E0B\u6307\u4EE4\u5B8C\u6210\u9A57\u8B49\uFF1A
\`\`\`
/verify ${bindCode}
\`\`\`
\u23F0 \u9A57\u8B49\u78BC\u5C07\u5728 10 \u5206\u9418\u5F8C\u5931\u6548\u3002`,
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    } catch (err) {
        console.error("Failed to create bind:", err);
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u7D81\u5B9A\u5931\u6557\uFF0C\u8ACB\u7A0D\u5F8C\u518D\u8A66",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(handleBindCommand, "handleBindCommand");
async function handleMessageComponent(interaction, env) {
    const customId = interaction.data.custom_id;
    if (customId.startsWith("players_page_")) {
        const offset = parseInt(customId.replace("players_page_", ""), 10) || 0;
        return buildPlayersResponse(env, offset, true);
    }
    if (customId.startsWith("tag_role_")) {
        return handleTagRoleButton(interaction, env);
    }
    if (customId === "status_refresh") {
        const statusResponse = await handleStatusCommand(interaction, env);
        const body = await statusResponse.json();
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.UPDATE_MESSAGE,
            data: body.data
        });
    }
    return Response.json({
        type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: {
            content: "\u274C \u672A\u77E5\u7684\u4E92\u52D5",
            flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
        }
    });
}
__name(handleMessageComponent, "handleMessageComponent");
function getOptionValue(options, name) {
    if (!options)
        return null;
    const option = options.find((o2) => o2.name === name);
    return option ? option.value : null;
}
__name(getOptionValue, "getOptionValue");
function generateBindCode() {
    return Math.random().toString(36).substring(2, 8).toUpperCase();
}
__name(generateBindCode, "generateBindCode");
var PAGE_SIZE = 10;
async function buildPlayersResponse(env, offset, isUpdate = false) {
    try {
        const total = await env.DB.prepare(
            "SELECT COUNT(*) as count FROM player_bindings WHERE mc_uuid IS NOT NULL"
        ).first();
        const players = await env.DB.prepare(
            "SELECT discord_name, mc_name, bound_at FROM player_bindings WHERE mc_uuid IS NOT NULL ORDER BY bound_at DESC LIMIT ? OFFSET ?"
        ).bind(PAGE_SIZE, offset).all();
        const totalCount = total?.count || 0;
        const totalPages = Math.ceil(totalCount / PAGE_SIZE) || 1;
        const currentPage = Math.floor(offset / PAGE_SIZE) + 1;
        let description = "";
        if (players.results.length === 0) {
            description = "\u76EE\u524D\u6C92\u6709\u5DF2\u7D81\u5B9A\u7684\u73A9\u5BB6";
        } else {
            description = players.results.map(
                (p2, i) => `**${offset + i + 1}.** ${p2.mc_name} \u2194 ${p2.discord_name}`
            ).join("\n");
        }
        const components = [];
        const buttons = [];
        if (offset > 0) {
            buttons.push({
                type: 2,
                style: 1,
                label: "\u25C0 \u4E0A\u4E00\u9801",
                custom_id: `players_page_${Math.max(0, offset - PAGE_SIZE)}`
            });
        }
        if (offset + PAGE_SIZE < totalCount) {
            buttons.push({
                type: 2,
                style: 1,
                label: "\u4E0B\u4E00\u9801 \u25B6",
                custom_id: `players_page_${offset + PAGE_SIZE}`
            });
        }
        if (buttons.length > 0) {
            components.push({ type: 1, components: buttons });
        }
        const responseType = isUpdate ? import_discord_interactions2.InteractionResponseType.UPDATE_MESSAGE : import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE;
        return Response.json({
            type: responseType,
            data: {
                embeds: [
                    {
                        title: "\u{1F465} \u5DF2\u7D81\u5B9A\u73A9\u5BB6\u5217\u8868",
                        description,
                        color: 5793266,
                        footer: { text: `\u7B2C ${currentPage} / ${totalPages} \u9801 \xB7 \u5171 ${totalCount} \u4F4D\u73A9\u5BB6` }
                    }
                ],
                components
            }
        });
    } catch (err) {
        console.error("Failed to fetch players:", err);
        const responseType = isUpdate ? import_discord_interactions2.InteractionResponseType.UPDATE_MESSAGE : import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE;
        return Response.json({
            type: responseType,
            data: {
                content: "\u274C \u7121\u6CD5\u53D6\u5F97\u73A9\u5BB6\u5217\u8868",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(buildPlayersResponse, "buildPlayersResponse");
async function handleSetChannelCommand(interaction, env) {
    const channelId = interaction.channel_id || interaction.channel?.id;
    const guildId = interaction.guild_id;
    const guildName = interaction.guild?.name || guildId;
    const userId = interaction.member?.user?.id || interaction.user?.id;
    if (!channelId || !guildId) {
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u6B64\u6307\u4EE4\u53EA\u80FD\u5728\u4F3A\u670D\u5668\u983B\u9053\u4E2D\u4F7F\u7528",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
    try {
        let channelName = channelId;
        try {
            const chRes = await fetch(`https://discord.com/api/v10/channels/${channelId}`, {
                headers: { Authorization: `Bot ${env.DISCORD_TOKEN}` }
            });
            if (chRes.ok) {
                const chData = await chRes.json();
                channelName = chData.name || channelId;
            }
        } catch (_) {
        }
        await env.DB.prepare(
            `INSERT INTO sync_channels (guild_id, guild_name, channel_id, channel_name, added_by)
       VALUES (?, ?, ?, ?, ?)
       ON CONFLICT (channel_id) DO UPDATE SET
         guild_name = excluded.guild_name,
         channel_name = excluded.channel_name,
         added_by = excluded.added_by,
         added_at = datetime('now')`
        ).bind(guildId, guildName, channelId, channelName, userId).run();
        const allChannels = await env.DB.prepare(
            "SELECT guild_name, channel_name, channel_id FROM sync_channels ORDER BY added_at ASC"
        ).all();
        const channelList = allChannels.results.map((c2) => `\u2022 **${c2.guild_name}** #${c2.channel_name}`).join("\n");
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                embeds: [
                    {
                        title: "\u2705 \u540C\u6B65\u983B\u9053\u5DF2\u8A2D\u5B9A",
                        description: `\u5DF2\u5C07 <#${channelId}> \u52A0\u5165 Minecraft \u804A\u5929\u540C\u6B65\u3002

**\u76EE\u524D\u540C\u6B65\u983B\u9053\uFF1A**
${channelList}`,
                        color: 65280
                    }
                ]
            }
        });
    } catch (err) {
        console.error("Failed to set channel:", err);
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u8A2D\u5B9A\u5931\u6557\uFF0C\u8ACB\u7A0D\u5F8C\u518D\u8A66",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(handleSetChannelCommand, "handleSetChannelCommand");
async function handleRemoveChannelCommand(interaction, env) {
    const channelId = interaction.channel_id || interaction.channel?.id;
    if (!channelId) {
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u6B64\u6307\u4EE4\u53EA\u80FD\u5728\u4F3A\u670D\u5668\u983B\u9053\u4E2D\u4F7F\u7528",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
    try {
        const existing = await env.DB.prepare(
            "SELECT * FROM sync_channels WHERE channel_id = ?"
        ).bind(channelId).first();
        if (!existing) {
            return Response.json({
                type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                data: {
                    content: "\u26A0\uFE0F \u6B64\u983B\u9053\u5C1A\u672A\u8A2D\u5B9A\u70BA\u540C\u6B65\u983B\u9053",
                    flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
                }
            });
        }
        await env.DB.prepare("DELETE FROM sync_channels WHERE channel_id = ?").bind(channelId).run();
        const remaining = await env.DB.prepare(
            "SELECT guild_name, channel_name FROM sync_channels ORDER BY added_at ASC"
        ).all();
        const channelList = remaining.results.length > 0 ? remaining.results.map((c2) => `\u2022 **${c2.guild_name}** #${c2.channel_name}`).join("\n") : "*(\u7121)*";
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                embeds: [
                    {
                        title: "\u{1F5D1}\uFE0F \u540C\u6B65\u983B\u9053\u5DF2\u79FB\u9664",
                        description: `\u5DF2\u5C07 <#${channelId}> \u5F9E Minecraft \u804A\u5929\u540C\u6B65\u4E2D\u79FB\u9664\u3002

**\u5269\u9918\u540C\u6B65\u983B\u9053\uFF1A**
${channelList}`,
                        color: 16753920
                    }
                ]
            }
        });
    } catch (err) {
        console.error("Failed to remove channel:", err);
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u79FB\u9664\u5931\u6557\uFF0C\u8ACB\u7A0D\u5F8C\u518D\u8A66",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(handleRemoveChannelCommand, "handleRemoveChannelCommand");
var ROLE_EMOJIS = ["1\uFE0F\u20E3", "2\uFE0F\u20E3", "3\uFE0F\u20E3", "4\uFE0F\u20E3", "5\uFE0F\u20E3"];
async function handleTagCommand(interaction, env) {
    const options = interaction.data.options || [];
    const guildId = interaction.guild_id;
    const resolvedRoles = interaction.data.resolved?.roles || {};
    const roles = [];
    for (const opt of options) {
        if (opt.name.startsWith("role") && resolvedRoles[opt.value]) {
            const role = resolvedRoles[opt.value];
            roles.push({
                id: opt.value,
                name: role.name,
                color: role.color || 5793266
            });
        }
    }
    if (roles.length === 0) {
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u8ACB\u81F3\u5C11\u9078\u64C7\u4E00\u500B\u8EAB\u5206\u7D44",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
    const title = getOptionValue(options, "title") || "\u9078\u64C7\u4F60\u7684\u8EAB\u5206\u7D44";
    const buttons = roles.map((role, i) => ({
        type: 2,
        // Button
        style: 1,
        // Primary
        label: role.name,
        emoji: { name: ROLE_EMOJIS[i] },
        custom_id: `tag_role_${role.id}`
    }));
    const description = roles.map((role, i) => `${ROLE_EMOJIS[i]} \u2014 <@&${role.id}>`).join("\n");
    return Response.json({
        type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
        data: {
            embeds: [
                {
                    title: `\u{1F3F7}\uFE0F ${title}`,
                    description: `\u9EDE\u64CA\u4E0B\u65B9\u6309\u9215\u4F86\u7372\u5F97\u6216\u79FB\u9664\u8EAB\u5206\u7D44\uFF1A

${description}`,
                    color: 5793266,
                    footer: { text: "\u518D\u6B21\u9EDE\u64CA\u5373\u53EF\u79FB\u9664\u8EAB\u5206\u7D44" }
                }
            ],
            components: [
                { type: 1, components: buttons }
                // Action Row
            ]
        }
    });
}
__name(handleTagCommand, "handleTagCommand");
async function handleTagRoleButton(interaction, env) {
    const customId = interaction.data.custom_id;
    const roleId = customId.replace("tag_role_", "");
    const userId = interaction.member?.user?.id;
    const guildId = interaction.guild_id;
    if (!userId || !guildId) {
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u7121\u6CD5\u53D6\u5F97\u4F7F\u7528\u8005\u6216\u4F3A\u670D\u5668\u8CC7\u8A0A",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
    const memberRoles = interaction.member?.roles || [];
    const hasRole = memberRoles.includes(roleId);
    try {
        const url = `https://discord.com/api/v10/guilds/${guildId}/members/${userId}/roles/${roleId}`;
        const method = hasRole ? "DELETE" : "PUT";
        const res = await fetch(url, {
            method,
            headers: {
                Authorization: `Bot ${env.DISCORD_TOKEN}`,
                "Content-Type": "application/json"
            }
        });
        if (!res.ok) {
            const errText = await res.text();
            console.error(`Failed to ${method} role:`, errText);
            if (res.status === 403) {
                return Response.json({
                    type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                    data: {
                        content: "\u274C Bot \u6B0A\u9650\u4E0D\u8DB3\uFF0C\u7121\u6CD5\u7BA1\u7406\u6B64\u8EAB\u5206\u7D44\u3002\n\u8ACB\u78BA\u8A8D Bot \u7684\u89D2\u8272\u4F4D\u7F6E\u9AD8\u65BC\u76EE\u6A19\u8EAB\u5206\u7D44\u3002",
                        flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
                    }
                });
            }
            return Response.json({
                type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                data: {
                    content: "\u274C \u64CD\u4F5C\u5931\u6557\uFF0C\u8ACB\u7A0D\u5F8C\u518D\u8A66",
                    flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
                }
            });
        }
        const action = hasRole ? "\u79FB\u9664" : "\u7372\u5F97";
        const emoji = hasRole ? "\u2796" : "\u2705";
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: `${emoji} \u5DF2${action}\u8EAB\u5206\u7D44 <@&${roleId}>`,
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    } catch (err) {
        console.error("handleTagRoleButton error:", err);
        return Response.json({
            type: import_discord_interactions2.InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
            data: {
                content: "\u274C \u64CD\u4F5C\u5931\u6557\uFF0C\u8ACB\u7A0D\u5F8C\u518D\u8A66",
                flags: import_discord_interactions2.InteractionResponseFlags.EPHEMERAL
            }
        });
    }
}
__name(handleTagRoleButton, "handleTagRoleButton");

// src/handlers/minecraft.js
async function postChat(request, env) {
    try {
        const { username, message } = await request.json();
        if (!username || !message) {
            return Response.json(
                { success: false, error: "Missing username or message" },
                { status: 400 }
            );
        }
        await env.DB.prepare(
            "INSERT INTO messages (source, username, content, delivered) VALUES (?, ?, ?, 1)"
        ).bind("minecraft", username, message).run();
        const channels = await env.DB.prepare(
            "SELECT channel_id FROM sync_channels"
        ).all();
        const channelIds = channels.results.map((c2) => c2.channel_id);
        if (channelIds.length === 0 && env.DISCORD_CHANNEL_ID) {
            channelIds.push(env.DISCORD_CHANNEL_ID);
        }
        if (channelIds.length === 0) {
            return Response.json(
                { success: false, error: "No sync channels configured" },
                { status: 400 }
            );
        }
        const messageBody = JSON.stringify({
            content: `**[MC] ${username}:** ${message}`
        });
        const token = env.DISCORD_TOKEN.trim();
        const results = await Promise.allSettled(
            channelIds.map(
                (chId) => fetch(`https://discord.com/api/v10/channels/${chId}/messages`, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bot ${token}`
                    },
                    body: messageBody
                })
            )
        );
        const failures = [];
        for (const [i, r2] of results.entries()) {
            if (r2.status === "rejected") {
                console.error(`Channel ${channelIds[i]} rejected:`, r2.reason);
                failures.push(r2);
            } else if (!r2.value.ok) {
                const errBody = await r2.value.text();
                console.error(`Channel ${channelIds[i]} HTTP ${r2.value.status}:`, errBody);
                failures.push(r2);
            }
        }
        if (failures.length > 0) {
            console.error(`Failed to send to ${failures.length}/${channelIds.length} channels`);
        }
        return Response.json({
            success: true,
            data: { sent: channelIds.length - failures.length, failed: failures.length }
        });
    } catch (err) {
        console.error("postChat error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(postChat, "postChat");
async function getMessages(request, env) {
    try {
        const url = new URL(request.url);
        const since = url.searchParams.get("since") || "1970-01-01T00:00:00";
        const limit = Math.min(parseInt(url.searchParams.get("limit") || "50", 10), 100);
        const messages = await env.DB.prepare(
            `SELECT id, username, content, created_at
       FROM messages
       WHERE source = 'discord' AND delivered = 0 AND created_at > ?
       ORDER BY created_at ASC
       LIMIT ?`
        ).bind(since, limit).all();
        return Response.json({
            success: true,
            data: messages.results
        });
    } catch (err) {
        console.error("getMessages error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(getMessages, "getMessages");
async function ackMessages(request, env) {
    try {
        const { ids } = await request.json();
        if (!Array.isArray(ids) || ids.length === 0) {
            return Response.json(
                { success: false, error: "Missing or empty ids array" },
                { status: 400 }
            );
        }
        const placeholders = ids.map(() => "?").join(",");
        await env.DB.prepare(
            `UPDATE messages SET delivered = 1 WHERE id IN (${placeholders})`
        ).bind(...ids).run();
        return Response.json({ success: true, data: { acknowledged: ids.length } });
    } catch (err) {
        console.error("ackMessages error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(ackMessages, "ackMessages");
async function getPlayers(request, env) {
    try {
        const players = await env.DB.prepare(
            "SELECT discord_id, discord_name, mc_uuid, mc_name, bound_at FROM player_bindings WHERE mc_uuid IS NOT NULL ORDER BY bound_at DESC"
        ).all();
        return Response.json({ success: true, data: players.results });
    } catch (err) {
        console.error("getPlayers error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(getPlayers, "getPlayers");
async function getPlayer(request, env) {
    try {
        const mcUuid = request.params.mc_uuid;
        const player = await env.DB.prepare(
            "SELECT discord_id, discord_name, mc_uuid, mc_name, bound_at FROM player_bindings WHERE mc_uuid = ?"
        ).bind(mcUuid).first();
        if (!player) {
            return Response.json(
                { success: false, error: "Player not found" },
                { status: 404 }
            );
        }
        return Response.json({ success: true, data: player });
    } catch (err) {
        console.error("getPlayer error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(getPlayer, "getPlayer");
async function bindPlayer(request, env) {
    try {
        const { mc_uuid, mc_name, bind_code } = await request.json();
        if (!mc_uuid || !mc_name || !bind_code) {
            return Response.json(
                { success: false, error: "Missing mc_uuid, mc_name, or bind_code" },
                { status: 400 }
            );
        }
        const binding = await env.DB.prepare(
            "SELECT * FROM player_bindings WHERE bind_code = ?"
        ).bind(bind_code).first();
        if (!binding) {
            return Response.json(
                { success: false, error: "Invalid bind code" },
                { status: 404 }
            );
        }
        const codeTime = (/* @__PURE__ */ new Date(binding.bind_code_at + "Z")).getTime();
        const now = Date.now();
        if (now - codeTime > 10 * 60 * 1e3) {
            await env.DB.prepare(
                "UPDATE player_bindings SET bind_code = NULL, bind_code_at = NULL WHERE id = ?"
            ).bind(binding.id).run();
            return Response.json(
                { success: false, error: "Bind code has expired" },
                { status: 410 }
            );
        }
        await env.DB.prepare(
            `UPDATE player_bindings
       SET mc_uuid = ?, mc_name = ?, bind_code = NULL, bind_code_at = NULL, bound_at = datetime('now')
       WHERE id = ?`
        ).bind(mc_uuid, mc_name, binding.id).run();
        return Response.json({
            success: true,
            data: {
                discord_id: binding.discord_id,
                discord_name: binding.discord_name,
                mc_uuid,
                mc_name
            }
        });
    } catch (err) {
        console.error("bindPlayer error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(bindPlayer, "bindPlayer");
async function getInventory(request, env) {
    try {
        const mcUuid = request.params.mc_uuid;
        const items = await env.DB.prepare(
            "SELECT item_id, item_name, quantity, metadata, updated_at FROM player_inventory WHERE mc_uuid = ? ORDER BY item_name ASC"
        ).bind(mcUuid).all();
        return Response.json({ success: true, data: items.results });
    } catch (err) {
        console.error("getInventory error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(getInventory, "getInventory");
async function putInventory(request, env) {
    try {
        const mcUuid = request.params.mc_uuid;
        const { items } = await request.json();
        if (!Array.isArray(items)) {
            return Response.json(
                { success: false, error: "items must be an array" },
                { status: 400 }
            );
        }
        const statements = [
            env.DB.prepare("DELETE FROM player_inventory WHERE mc_uuid = ?").bind(mcUuid)
        ];
        for (const item of items) {
            statements.push(
                env.DB.prepare(
                    `INSERT INTO player_inventory (mc_uuid, item_id, item_name, quantity, metadata, updated_at)
           VALUES (?, ?, ?, ?, ?, datetime('now'))`
                ).bind(
                    mcUuid,
                    item.item_id,
                    item.item_name,
                    item.quantity || 1,
                    item.metadata ? JSON.stringify(item.metadata) : null
                )
            );
        }
        await env.DB.batch(statements);
        return Response.json({ success: true, data: { count: items.length } });
    } catch (err) {
        console.error("putInventory error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(putInventory, "putInventory");
async function patchInventoryItem(request, env) {
    try {
        const { mc_uuid, item_id } = request.params;
        const { quantity, item_name, metadata } = await request.json();
        if (quantity === void 0) {
            return Response.json(
                { success: false, error: "Missing quantity" },
                { status: 400 }
            );
        }
        if (quantity <= 0) {
            await env.DB.prepare(
                "DELETE FROM player_inventory WHERE mc_uuid = ? AND item_id = ?"
            ).bind(mc_uuid, item_id).run();
            return Response.json({ success: true, data: { deleted: true } });
        }
        await env.DB.prepare(
            `INSERT INTO player_inventory (mc_uuid, item_id, item_name, quantity, metadata, updated_at)
       VALUES (?, ?, ?, ?, ?, datetime('now'))
       ON CONFLICT (mc_uuid, item_id) DO UPDATE SET
         quantity = excluded.quantity,
         item_name = COALESCE(excluded.item_name, item_name),
         metadata = COALESCE(excluded.metadata, metadata),
         updated_at = excluded.updated_at`
        ).bind(
            mc_uuid,
            item_id,
            item_name || item_id,
            quantity,
            metadata ? JSON.stringify(metadata) : null
        ).run();
        return Response.json({ success: true });
    } catch (err) {
        console.error("patchInventoryItem error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(patchInventoryItem, "patchInventoryItem");
async function getSettings(request, env) {
    try {
        const settings = await env.DB.prepare(
            "SELECT key, value, updated_at FROM server_settings ORDER BY key ASC"
        ).all();
        return Response.json({ success: true, data: settings.results });
    } catch (err) {
        console.error("getSettings error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(getSettings, "getSettings");
async function getSetting(request, env) {
    try {
        const key = request.params.key;
        const setting = await env.DB.prepare(
            "SELECT key, value, updated_at FROM server_settings WHERE key = ?"
        ).bind(key).first();
        if (!setting) {
            return Response.json(
                { success: false, error: "Setting not found" },
                { status: 404 }
            );
        }
        return Response.json({ success: true, data: setting });
    } catch (err) {
        console.error("getSetting error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(getSetting, "getSetting");
async function putSetting(request, env) {
    try {
        const key = request.params.key;
        const { value } = await request.json();
        if (value === void 0) {
            return Response.json(
                { success: false, error: "Missing value" },
                { status: 400 }
            );
        }
        await env.DB.prepare(
            `INSERT INTO server_settings (key, value, updated_at)
       VALUES (?, ?, datetime('now'))
       ON CONFLICT (key) DO UPDATE SET
         value = excluded.value,
         updated_at = excluded.updated_at`
        ).bind(key, String(value)).run();
        return Response.json({ success: true });
    } catch (err) {
        console.error("putSetting error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(putSetting, "putSetting");
async function postServerStatus(request, env) {
    try {
        const body = await request.json();
        const keyMap = {
            status: "server_status",
            tps: "server_tps",
            players_online: "server_players_online",
            players_max: "server_players_max",
            version: "server_version"
        };
        const statements = [];
        for (const [bodyKey, settingKey] of Object.entries(keyMap)) {
            if (body[bodyKey] !== void 0) {
                statements.push(
                    env.DB.prepare(
                        `INSERT INTO server_settings (key, value, updated_at)
             VALUES (?, ?, datetime('now'))
             ON CONFLICT (key) DO UPDATE SET
               value = excluded.value,
               updated_at = excluded.updated_at`
                    ).bind(settingKey, String(body[bodyKey]))
                );
            }
        }
        if (statements.length > 0) {
            await env.DB.batch(statements);
        }
        return Response.json({ success: true });
    } catch (err) {
        console.error("postServerStatus error:", err);
        return Response.json(
            { success: false, error: "Internal server error" },
            { status: 500 }
        );
    }
}
__name(postServerStatus, "postServerStatus");

// src/server.js
var router = n();
router.post("/", async (request, env) => {
    const { isValid, interaction } = await verifyDiscordRequest(request, env);
    if (!isValid || !interaction) {
        return new Response("Bad request signature", { status: 401 });
    }
    return handleDiscordInteraction(interaction, env);
});
router.all("/api/mc/*", (request, env) => {
    return authenticateMinecraft(request, env);
});
router.post("/api/mc/chat", postChat);
router.get("/api/mc/messages", getMessages);
router.post("/api/mc/messages/ack", ackMessages);
router.get("/api/mc/players", getPlayers);
router.post("/api/mc/players/bind", bindPlayer);
router.get("/api/mc/players/:mc_uuid", getPlayer);
router.get("/api/mc/inventory/:mc_uuid", getInventory);
router.put("/api/mc/inventory/:mc_uuid", putInventory);
router.patch("/api/mc/inventory/:mc_uuid/:item_id", patchInventoryItem);
router.get("/api/mc/settings", getSettings);
router.get("/api/mc/settings/:key", getSetting);
router.put("/api/mc/settings/:key", putSetting);
router.post("/api/mc/server/status", postServerStatus);
router.get("/health", () => {
    return Response.json({ status: "ok", timestamp: (/* @__PURE__ */ new Date()).toISOString() });
});
router.all("*", () => {
    return new Response("Not Found", { status: 404 });
});
var server_default = { ...router };
export {
    server_default as default
};
//# sourceMappingURL=server.js.map
