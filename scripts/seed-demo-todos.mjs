#!/usr/bin/env node

import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';

const DEFAULT_MANIFEST_PATH = '.whim/demo-starter-todos.json';
const PAGE_LIMIT = 50;
const BATCH_LIMIT = 10;
const VALID_PRIORITIES = new Set(['low', 'medium', 'high', 'urgent']);
const VALID_EFFORTS = new Set(['small', 'medium', 'large']);
const ALLOWED_TODO_KEYS = new Set(['title', 'body', 'tags', 'priority', 'effort']);

function printUsage() {
  console.log(`Usage:
  npm run seed:todos -- --mcp-url <url> --token <token> [--manifest <path>] [--dry-run]

Environment:
  WHIM_MCP_URL    Workspace-scoped Whim MCP URL
  WHIM_MCP_TOKEN  Workspace-scoped Whim MCP token
`);
}

function parseArgs(argv) {
  const options = {
    manifestPath: DEFAULT_MANIFEST_PATH,
    dryRun: false,
    mcpUrl: process.env.WHIM_MCP_URL || '',
    token: process.env.WHIM_MCP_TOKEN || '',
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--help' || arg === '-h') {
      options.help = true;
      continue;
    }
    if (arg === '--dry-run') {
      options.dryRun = true;
      continue;
    }
    if (arg === '--manifest') {
      options.manifestPath = readFlagValue(argv, index, arg);
      index += 1;
      continue;
    }
    if (arg === '--mcp-url') {
      options.mcpUrl = readFlagValue(argv, index, arg);
      index += 1;
      continue;
    }
    if (arg === '--token') {
      options.token = readFlagValue(argv, index, arg);
      index += 1;
      continue;
    }
    throw new Error(`Unknown argument: ${arg}`);
  }

  return options;
}

function readFlagValue(argv, index, flag) {
  const value = argv[index + 1];
  if (!value || value.startsWith('--')) {
    throw new Error(`${flag} requires a value`);
  }
  return value;
}

function readNonEmptyString(value, field, index) {
  if (typeof value !== 'string') {
    throw new Error(`todos[${index}].${field} must be a string`);
  }
  const trimmed = value.trim();
  if (!trimmed) {
    throw new Error(`todos[${index}].${field} must not be empty`);
  }
  return trimmed;
}

function readOptionalString(value, field, index) {
  if (value === undefined) return undefined;
  if (typeof value !== 'string') {
    throw new Error(`todos[${index}].${field} must be a string`);
  }
  const trimmed = value.trim();
  return trimmed || undefined;
}

function readTags(value, index) {
  if (value === undefined) return undefined;
  if (!Array.isArray(value)) {
    throw new Error(`todos[${index}].tags must be an array`);
  }

  const tags = value.map((tag, tagIndex) => {
    if (typeof tag !== 'string') {
      throw new Error(`todos[${index}].tags[${tagIndex}] must be a string`);
    }
    return tag.trim();
  }).filter(Boolean);

  return tags.length > 0 ? [...new Set(tags)] : undefined;
}

function validateTodo(rawTodo, index) {
  if (!rawTodo || typeof rawTodo !== 'object' || Array.isArray(rawTodo)) {
    throw new Error(`todos[${index}] must be an object`);
  }

  for (const key of Object.keys(rawTodo)) {
    if (!ALLOWED_TODO_KEYS.has(key)) {
      throw new Error(`todos[${index}] has unsupported field "${key}"`);
    }
  }

  const title = readNonEmptyString(rawTodo.title, 'title', index);
  const body = readOptionalString(rawTodo.body, 'body', index);
  const tags = readTags(rawTodo.tags, index);
  const priority = readOptionalString(rawTodo.priority, 'priority', index);
  const effort = readOptionalString(rawTodo.effort, 'effort', index);

  if (priority && !VALID_PRIORITIES.has(priority)) {
    throw new Error(`todos[${index}].priority must be one of ${[...VALID_PRIORITIES].join(', ')}`);
  }
  if (effort && !VALID_EFFORTS.has(effort)) {
    throw new Error(`todos[${index}].effort must be one of ${[...VALID_EFFORTS].join(', ')}`);
  }

  return {
    title,
    ...(body ? { body } : {}),
    ...(tags ? { tags } : {}),
    ...(priority ? { priority } : {}),
    ...(effort ? { effort } : {}),
  };
}

async function readManifest(manifestPath) {
  const absolutePath = path.resolve(process.cwd(), manifestPath);
  const parsed = JSON.parse(await readFile(absolutePath, 'utf8'));
  if (!parsed || typeof parsed !== 'object' || !Array.isArray(parsed.todos)) {
    throw new Error('Manifest must contain a todos array');
  }

  const todos = parsed.todos.map(validateTodo);
  if (todos.length === 0) {
    throw new Error('Manifest must contain at least one todo');
  }

  const seenTitles = new Set();
  for (const todo of todos) {
    if (seenTitles.has(todo.title)) {
      throw new Error(`Duplicate todo title in manifest: ${todo.title}`);
    }
    seenTitles.add(todo.title);
  }

  return todos;
}

function extractText(result) {
  const content = Array.isArray(result.content) ? result.content : [];
  return content
    .filter((item) => item && item.type === 'text' && typeof item.text === 'string')
    .map((item) => item.text)
    .join('\n');
}

async function callToolText(client, name, args) {
  const result = await client.callTool({ name, arguments: args });
  const text = extractText(result);
  if (result.isError) {
    throw new Error(text || `${name} failed`);
  }
  return text;
}

function extractTaskTitles(listTasksText) {
  const titles = [];
  for (const line of listTasksText.split(/\r?\n/)) {
    const match = line.match(/^\[[^\]]+\]\s+(.+)$/);
    if (match) {
      titles.push(match[1].trim());
    }
  }
  return titles;
}

async function listExistingTitles(client) {
  const titles = new Set();
  let offset = 0;

  while (true) {
    const text = await callToolText(client, 'list_tasks', {
      status: 'all',
      limit: PAGE_LIMIT,
      offset,
    });
    const pageTitles = extractTaskTitles(text);
    for (const title of pageTitles) {
      titles.add(title);
    }

    if (pageTitles.length < PAGE_LIMIT) {
      break;
    }
    offset += PAGE_LIMIT;
  }

  return titles;
}

function chunk(items, size) {
  const chunks = [];
  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }
  return chunks;
}

async function createMissingTodos(client, todos) {
  const outputs = [];
  for (const batch of chunk(todos, BATCH_LIMIT)) {
    const text = await callToolText(client, 'create_batch', {
      kind: 'todo',
      relationship: 'top_level',
      items: batch,
    });
    outputs.push(text);
  }
  return outputs;
}

async function main() {
  const options = parseArgs(process.argv.slice(2));
  if (options.help) {
    printUsage();
    return;
  }
  if (!options.mcpUrl) {
    throw new Error('Missing --mcp-url or WHIM_MCP_URL');
  }
  if (!options.token) {
    throw new Error('Missing --token or WHIM_MCP_TOKEN');
  }

  const todos = await readManifest(options.manifestPath);
  const transport = new StreamableHTTPClientTransport(new URL(options.mcpUrl), {
    requestInit: {
      headers: {
        Authorization: `Bearer ${options.token}`,
      },
    },
  });
  const client = new Client({
    name: 'whim-demo-todo-seeder',
    version: '0.1.0',
  });
  let connected = false;

  try {
    await client.connect(transport);
    connected = true;
    const existingTitles = await listExistingTitles(client);
    const missingTodos = todos.filter((todo) => !existingTitles.has(todo.title));

    console.log(`Manifest todos: ${todos.length}`);
    console.log(`Existing matching titles: ${todos.length - missingTodos.length}`);
    console.log(`Missing todos: ${missingTodos.length}`);

    if (missingTodos.length === 0) {
      console.log('No todos to create.');
      return;
    }

    for (const todo of missingTodos) {
      console.log(`- ${todo.title}`);
    }

    if (options.dryRun) {
      console.log('Dry run only. No todos created.');
      return;
    }

    await createMissingTodos(client, missingTodos);
    console.log(`Created ${missingTodos.length} todo${missingTodos.length === 1 ? '' : 's'}.`);
  } finally {
    if (connected) {
      await client.close();
    }
  }
}

main().catch((error) => {
  const relativeScript = path.relative(process.cwd(), fileURLToPath(import.meta.url));
  console.error(`${relativeScript}: ${error instanceof Error ? error.message : String(error)}`);
  process.exit(1);
});
