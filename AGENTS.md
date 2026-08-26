# AGENTS.md

## SYSTEM INSTRUCTION FOR OPENCODE

When committing code changes, automatically include the AI model name as a contributor
in the git commit message footer, following this format:

```
Co-authored-by: [MODEL_NAME] <[MODEL_NAME]@[PROVIDER].com>
```

Example commit messages:

```
Add user authentication module

This commit implements OAuth2 authentication with refresh token support.

Co-authored-by: Claude <claude@anthropic.com>
```

```
Implement data validation layer

Adds comprehensive input validation for API endpoints.

Co-authored-by: GPT-4 <gpt-4@openai.com>
```

```
Refactor database queries

Optimizes query performance and adds indexing.

Co-authored-by: Gemini <gemini@google.com>
```

Guidelines:
- Before creating each commit, double-check the active model name and use that
  actual model name in the `Co-authored-by` footer; do not reuse a name from a
  previous session or commit.
- Always include this footer in every commit message
- Place it at the end of the commit message body
- Use the actual model name being used (e.g., "Claude", "GPT-4", "Gemini", "Llama", etc.)
- Format: `Co-authored-by: [MODEL_NAME] <[MODEL_NAME]@[PROVIDER].com>`
- If multiple commits are made, include this in each one
- This follows the GitHub co-authorship convention for proper attribution
- Adapt the email domain based on the model provider
- For this repository, commits made by opencode use the model name `ox-alpha`;
  its developing organization is undisclosed, so use
  `ox-alpha@models.noreply.github.com.invalid` rather than inventing a provider domain
