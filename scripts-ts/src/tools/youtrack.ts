/**
 * YouTrack API utilities for IdeaVim scripts
 */

const YOUTRACK_BASE_URL = "https://youtrack.jetbrains.com/api";
const CLAUDE_ANALYZED_TAG_ID = "68-507461";
const CLAUDE_PENDING_CLARIFICATION_TAG_ID = "68-507582";
const JETBRAINS_TEAM_GROUP_ID = "10-3";
const RELEASED_IN_EAP_TAG_ID = "68-385032";
const VIM_PROJECT_ID = "22-43";
const FIX_VERSIONS_FIELD_ID = "123-285";

export interface TicketDetails {
  id: string;
  summary: string;
  description: string | null;
  state: string;
  created: string;
}

export interface TicketComment {
  author: string;
  text: string;
  created: string;
}

export interface TicketAttachment {
  name: string;
  url: string;
  mimeType: string | null;
}

function getToken(): string {
  const token = process.env.YOUTRACK_TOKEN;
  if (!token) {
    throw new Error("YOUTRACK_TOKEN environment variable is not set");
  }
  return token;
}

async function youtrackFetch(
  endpoint: string,
  options: RequestInit = {}
): Promise<Response> {
  const url = `${YOUTRACK_BASE_URL}${endpoint}`;
  const response = await fetch(url, {
    ...options,
    headers: {
      Authorization: `Bearer ${getToken()}`,
      Accept: "application/json",
      "Content-Type": "application/json",
      ...options.headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`YouTrack API error: ${response.status} ${text}`);
  }

  return response;
}

export async function getTicketsByQuery(query: string): Promise<string[]> {
  const params = new URLSearchParams({
    fields: "idReadable",
    query: `project:VIM ${query}`,
  });

  const response = await youtrackFetch(`/issues/?${params}`);
  const data = await response.json();

  return data.map((issue: { idReadable: string }) => issue.idReadable);
}

export async function getTicketDetails(
  ticketId: string
): Promise<TicketDetails> {
  const params = new URLSearchParams({
    fields: "idReadable,summary,description,created,customFields(name,value(name))",
  });

  const response = await youtrackFetch(`/issues/${ticketId}?${params}`);
  const data = await response.json();

  const stateField = data.customFields?.find(
    (f: { name: string }) => f.name === "State"
  );
  const state = stateField?.value?.name ?? "Unknown";

  return {
    id: data.idReadable,
    summary: data.summary,
    description: data.description ?? null,
    state,
    created: new Date(data.created).toISOString(),
  };
}

export async function getTicketComments(
  ticketId: string
): Promise<TicketComment[]> {
  const params = new URLSearchParams({
    fields: "author(name),text,created",
  });

  const response = await youtrackFetch(`/issues/${ticketId}/comments?${params}`);
  const data = await response.json();

  return data.map((comment: { author?: { name?: string }; text: string; created: number }) => ({
    author: comment.author?.name ?? "Unknown",
    text: comment.text,
    created: new Date(comment.created).toISOString(),
  }));
}

export async function getTicketAttachments(
  ticketId: string
): Promise<TicketAttachment[]> {
  const params = new URLSearchParams({
    fields: "name,url,mimeType",
  });

  const response = await youtrackFetch(`/issues/${ticketId}/attachments?${params}`);
  const data = await response.json();

  return data.map((attachment: { name: string; url: string; mimeType?: string }) => ({
    name: attachment.name,
    url: `https://youtrack.jetbrains.com${attachment.url}`,
    mimeType: attachment.mimeType ?? null,
  }));
}

export async function setTag(ticketId: string, tagId: string): Promise<void> {
  console.log(`Adding tag ${tagId} to ${ticketId}...`);

  await youtrackFetch(`/issues/${ticketId}/tags`, {
    method: "POST",
    body: JSON.stringify({ id: tagId }),
  });

  console.log(`Tag added successfully`);
}

export async function removeTag(ticketId: string, tagId: string): Promise<void> {
  console.log(`Removing tag ${tagId} from ${ticketId}...`);

  await youtrackFetch(`/issues/${ticketId}/tags/${tagId}`, {
    method: "DELETE",
  });

  console.log(`Tag removed successfully`);
}

export async function setStatus(
  ticketId: string,
  status: string
): Promise<void> {
  console.log(`Setting ${ticketId} status to "${status}"...`);

  const response = await youtrackFetch(
    `/issues/${ticketId}?fields=customFields(name,value(name))`,
    {
      method: "POST",
      body: JSON.stringify({
        customFields: [
          {
            name: "State",
            $type: "SingleEnumIssueCustomField",
            value: { name: status },
          },
        ],
      }),
    }
  );

  const data = await response.json();

  // Verify the status was set correctly
  const stateField = data.customFields?.find(
    (f: { name: string }) => f.name === "State"
  );
  const finalState = stateField?.value?.name;

  if (finalState !== status) {
    throw new Error(
      `Ticket ${ticketId} status not updated! Expected "${status}", got "${finalState}"`
    );
  }

  console.log(`Status set successfully to "${status}"`);
}

export async function addComment(
  ticketId: string,
  text: string,
  isPrivate: boolean = false
): Promise<void> {
  console.log(`Adding ${isPrivate ? "private " : ""}comment to ${ticketId}...`);

  const body: Record<string, unknown> = { text };

  if (isPrivate) {
    body.visibility = {
      $type: "LimitedVisibility",
      permittedGroups: [{ id: JETBRAINS_TEAM_GROUP_ID }],
    };
  }

  await youtrackFetch(`/issues/${ticketId}/comments`, {
    method: "POST",
    body: JSON.stringify(body),
  });

  console.log(`Comment added successfully`);
}

export async function downloadAttachment(
  attachmentUrl: string,
  localPath: string
): Promise<boolean> {
  try {
    console.log(`Downloading attachment to ${localPath}...`);

    const response = await fetch(attachmentUrl, {
      headers: {
        Authorization: `Bearer ${getToken()}`,
      },
    });

    if (!response.ok) {
      console.error(`Failed to download attachment: ${response.status}`);
      return false;
    }

    const buffer = await response.arrayBuffer();
    const { writeFileSync } = await import("fs");
    writeFileSync(localPath, Buffer.from(buffer));

    console.log(`Downloaded attachment successfully`);
    return true;
  } catch (error) {
    console.error(`Error downloading attachment: ${error}`);
    return false;
  }
}

// Release management functions

export async function createReleaseVersion(name: string): Promise<string> {
  console.log(`Creating new release version in YouTrack: ${name}`);

  const response = await youtrackFetch(
    `/admin/projects/${VIM_PROJECT_ID}/customFields/${FIX_VERSIONS_FIELD_ID}/bundle/values?fields=id,name`,
    {
      method: "POST",
      body: JSON.stringify({
        name,
        $type: "VersionBundleElement",
      }),
    }
  );

  const data = await response.json();
  const versionId = data.id;

  console.log(`Created release version "${name}" with ID: ${versionId}`);
  return versionId;
}

export async function getVersionIdByName(name: string): Promise<string | null> {
  console.log(`Looking up version ID for: ${name}`);

  const params = new URLSearchParams({
    fields: "id,name",
    query: name,
  });

  const response = await youtrackFetch(
    `/admin/projects/${VIM_PROJECT_ID}/customFields/${FIX_VERSIONS_FIELD_ID}/bundle/values?${params}`
  );

  const data = await response.json();

  if (data.length === 0) {
    console.log(`Version "${name}" not found`);
    return null;
  }

  const versionId = data[0].id;
  console.log(`Found version "${name}" with ID: ${versionId}`);
  return versionId;
}

export async function deleteVersionById(id: string): Promise<void> {
  console.log(`Deleting version with ID: ${id}`);

  await youtrackFetch(
    `/admin/projects/${VIM_PROJECT_ID}/customFields/${FIX_VERSIONS_FIELD_ID}/bundle/values/${id}`,
    {
      method: "DELETE",
    }
  );

  console.log(`Version deleted successfully`);
}

export async function setFixVersion(
  ticketId: string,
  version: string
): Promise<void> {
  console.log(`Setting fix version "${version}" for ${ticketId}...`);

  const response = await youtrackFetch(
    `/issues/${ticketId}?fields=customFields(name,value(name))`,
    {
      method: "POST",
      body: JSON.stringify({
        customFields: [
          {
            name: "Fix versions",
            $type: "MultiVersionIssueCustomField",
            value: [{ name: version }],
          },
        ],
      }),
    }
  );

  const data = await response.json();

  // Verify the fix version was set correctly
  const fixVersionsField = data.customFields?.find(
    (f: { name: string }) => f.name === "Fix versions"
  );
  const versions = fixVersionsField?.value ?? [];
  const hasVersion = versions.some(
    (v: { name: string }) => v.name === version
  );

  if (!hasVersion) {
    throw new Error(
      `Ticket ${ticketId} fix version not updated! Expected "${version}" to be in fix versions`
    );
  }

  console.log(`Fix version set successfully to "${version}"`);
}

export {
  CLAUDE_ANALYZED_TAG_ID,
  CLAUDE_PENDING_CLARIFICATION_TAG_ID,
  RELEASED_IN_EAP_TAG_ID,
  VIM_PROJECT_ID,
  FIX_VERSIONS_FIELD_ID,
};
