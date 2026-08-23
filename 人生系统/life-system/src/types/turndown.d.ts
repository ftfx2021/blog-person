declare module "turndown" {
  export default class TurndownService {
    constructor(options?: { headingStyle?: "setext" | "atx"; codeBlockStyle?: "indented" | "fenced" });
    turndown(input: string): string;
  }
}
