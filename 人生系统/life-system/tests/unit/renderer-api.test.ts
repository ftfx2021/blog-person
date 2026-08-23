import { describe, expect, it } from "vitest";
import { reactive } from "vue";
import { toIpcPayload } from "../../src/shared/contracts/ipc-payload.js";

describe("renderer IPC payload", () => {
  it("removes Vue reactive Proxy before crossing contextBridge", () => {
    const source = reactive({
      provider: "ollama",
      nested: { model: "qwen3-embedding:0.6b" },
    });

    const payload = toIpcPayload(source);

    expect(payload).toEqual({
      provider: "ollama",
      nested: { model: "qwen3-embedding:0.6b" },
    });
    expect(payload).not.toBe(source);
  });
});
