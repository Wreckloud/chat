import {s as p} from "./request-DYqQOd03.js";
import {
    _ as K,
    r as m,
    a as E,
    C as Y,
    y as j,
    c as q,
    b as k,
    d as a,
    w as s,
    f as g,
    E as o,
    B as F,
    o as w,
    A as $,
    k as R,
    h as f
} from "./index-C5wDznsf.js";

function G(l) {
    return p({url: "/admin/forum/threads", method: "get", params: l})
}

function H(l) {
    return p({url: "/admin/forum/replies", method: "get", params: l})
}

function J(l, d) {
    return p({url: `/admin/forum/threads/${l}/lock`, method: "put", data: {locked: d}})
}

function Q(l, d) {
    return p({url: `/admin/forum/threads/${l}/sticky`, method: "put", data: {sticky: d}})
}

function W(l, d) {
    return p({url: `/admin/forum/threads/${l}/essence`, method: "put", data: {essence: d}})
}

function X(l) {
    return p({url: `/admin/forum/threads/${l}`, method: "delete"})
}

function Z(l) {
    return p({url: `/admin/forum/replies/${l}`, method: "delete"})
}

const ee = {class: "content-page"}, te = {class: "page-card table-card"}, ae = {class: "pager-wrap"},
    le = {class: "pager-wrap"}, ne = {
        __name: "index", setup(l) {
            const d = m("thread"), v = m(!1), b = m(!1), C = m([]), z = m([]), i = E({page: 1, size: 20, total: 0}),
                r = E({page: 1, size: 20, total: 0});

            async function u() {
                v.value = !0;
                try {
                    const e = await G({page: i.page, size: i.size});
                    C.value = (e == null ? void 0 : e.list) || [], i.total = Number((e == null ? void 0 : e.total) || 0)
                } catch (e) {
                    o.error((e == null ? void 0 : e.message) || "加载主题失败")
                } finally {
                    v.value = !1
                }
            }

            async function _() {
                b.value = !0;
                try {
                    const e = await H({page: r.page, size: r.size});
                    z.value = (e == null ? void 0 : e.list) || [], r.total = Number((e == null ? void 0 : e.total) || 0)
                } catch (e) {
                    o.error((e == null ? void 0 : e.message) || "加载回复失败")
                } finally {
                    b.value = !1
                }
            }

            async function S(e) {
                try {
                    await J(e.threadId, e.status !== "LOCKED"), o.success("操作成功"), u()
                } catch (t) {
                    o.error((t == null ? void 0 : t.message) || "操作失败")
                }
            }

            async function D(e) {
                if (e.threadType !== "ANNOUNCEMENT") try {
                    await Q(e.threadId, e.threadType !== "STICKY"), o.success("操作成功"), u()
                } catch (t) {
                    o.error((t == null ? void 0 : t.message) || "操作失败")
                }
            }

            async function P(e) {
                try {
                    await W(e.threadId, e.isEssence !== !0), o.success("操作成功"), u()
                } catch (t) {
                    o.error((t == null ? void 0 : t.message) || "操作失败")
                }
            }

            async function V(e) {
                try {
                    await X(e.threadId), o.success("删除成功"), C.value.length === 1 && i.page > 1 && (i.page -= 1), u()
                } catch (t) {
                    o.error((t == null ? void 0 : t.message) || "删除失败")
                }
            }

            async function B(e) {
                try {
                    await Z(e.replyId), o.success("删除成功"), z.value.length === 1 && r.page > 1 && (r.page -= 1), _()
                } catch (t) {
                    o.error((t == null ? void 0 : t.message) || "删除失败")
                }
            }

            function M(e) {
                i.page = e, u()
            }

            function A(e) {
                i.size = e, i.page = 1, u()
            }

            function L(e) {
                r.page = e, _()
            }

            function O(e) {
                r.size = e, r.page = 1, _()
            }

            return Y(d, e => {
                if (e === "thread") {
                    u();
                    return
                }
                _()
            }), j(() => {
                u()
            }), (e, t) => {
                const n = g("el-table-column"), h = g("el-button"), T = g("el-table"), N = g("el-pagination"),
                    I = g("el-tab-pane"), U = g("el-tabs"), x = F("loading");
                return w(), q("div", ee, [k("div", te, [a(U, {
                    modelValue: d.value,
                    "onUpdate:modelValue": t[0] || (t[0] = c => d.value = c)
                }, {
                    default: s(() => [a(I, {label: "主题治理", name: "thread"}, {
                        default: s(() => [$((w(), R(T, {
                            data: C.value,
                            stripe: ""
                        }, {
                            default: s(() => [a(n, {prop: "threadId", label: "主题ID", width: "90"}), a(n, {
                                prop: "title",
                                label: "标题",
                                "min-width": "220"
                            }), a(n, {prop: "authorNickname", label: "作者", width: "130"}), a(n, {
                                prop: "status",
                                label: "状态",
                                width: "100"
                            }), a(n, {prop: "replyCount", label: "回复", width: "80"}), a(n, {
                                prop: "likeCount",
                                label: "点赞",
                                width: "80"
                            }), a(n, {
                                label: "操作",
                                width: "330",
                                fixed: "right"
                            }, {
                                default: s(({row: c}) => [a(h, {
                                    size: "small",
                                    onClick: y => S(c)
                                }, {
                                    default: s(() => [...t[1] || (t[1] = [f("锁帖", -1)])]),
                                    _: 1
                                }, 8, ["onClick"]), a(h, {
                                    size: "small",
                                    disabled: c.threadType === "ANNOUNCEMENT",
                                    onClick: y => D(c)
                                }, {
                                    default: s(() => [...t[2] || (t[2] = [f("置顶", -1)])]),
                                    _: 1
                                }, 8, ["disabled", "onClick"]), a(h, {
                                    size: "small",
                                    onClick: y => P(c)
                                }, {
                                    default: s(() => [...t[3] || (t[3] = [f("精华", -1)])]),
                                    _: 1
                                }, 8, ["onClick"]), a(h, {
                                    size: "small",
                                    type: "danger",
                                    onClick: y => V(c)
                                }, {default: s(() => [...t[4] || (t[4] = [f("删除", -1)])]), _: 1}, 8, ["onClick"])]), _: 1
                            })]), _: 1
                        }, 8, ["data"])), [[x, v.value]]), k("div", ae, [a(N, {
                            background: "",
                            layout: "total, prev, pager, next, sizes",
                            total: i.total,
                            "current-page": i.page,
                            "page-size": i.size,
                            "page-sizes": [10, 20, 50],
                            onCurrentChange: M,
                            onSizeChange: A
                        }, null, 8, ["total", "current-page", "page-size"])])]), _: 1
                    }), a(I, {label: "回复治理", name: "reply"}, {
                        default: s(() => [$((w(), R(T, {
                            data: z.value,
                            stripe: ""
                        }, {
                            default: s(() => [a(n, {prop: "replyId", label: "回复ID", width: "90"}), a(n, {
                                prop: "threadId",
                                label: "主题ID",
                                width: "90"
                            }), a(n, {prop: "authorNickname", label: "作者", width: "130"}), a(n, {
                                prop: "content",
                                label: "内容",
                                "min-width": "280"
                            }), a(n, {prop: "likeCount", label: "点赞", width: "80"}), a(n, {
                                label: "操作",
                                width: "120",
                                fixed: "right"
                            }, {
                                default: s(({row: c}) => [a(h, {
                                    size: "small",
                                    type: "danger",
                                    onClick: y => B(c)
                                }, {default: s(() => [...t[5] || (t[5] = [f("删除", -1)])]), _: 1}, 8, ["onClick"])]), _: 1
                            })]), _: 1
                        }, 8, ["data"])), [[x, b.value]]), k("div", le, [a(N, {
                            background: "",
                            layout: "total, prev, pager, next, sizes",
                            total: r.total,
                            "current-page": r.page,
                            "page-size": r.size,
                            "page-sizes": [10, 20, 50],
                            onCurrentChange: L,
                            onSizeChange: O
                        }, null, 8, ["total", "current-page", "page-size"])])]), _: 1
                    })]), _: 1
                }, 8, ["modelValue"])])])
            }
        }
    }, ie = K(ne, [["__scopeId", "data-v-df1b3c4c"]]);
export {ie as default};
