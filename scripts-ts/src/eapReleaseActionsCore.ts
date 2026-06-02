import {
  addComment,
  getTicketsByQuery,
  RELEASED_IN_EAP_TAG_ID,
  setTag,
} from "./tools/youtrack.js";

export const READY_TO_RELEASE_NOT_IN_EAP_QUERY =
  "#{Ready To Release} tag: -{IdeaVim Released In EAP}";

export function resolveReleaseVersion(args: string[], env: NodeJS.ProcessEnv): string {
  const version = args[0] || env.ORG_GRADLE_PROJECT_version;
  if (!version) {
    throw new Error(
      "Usage: eapReleaseActions.ts [version]\n" +
        "Or set ORG_GRADLE_PROJECT_version in the environment.",
    );
  }
  return version;
}

export function buildEapReleaseComment(version: string): string {
  return `The fix is available in the IdeaVim ${version}. See https://jb.gg/ideavim-eap for the instructions on how to get EAP builds as updates within the IDE. You can also wait till the next stable release with this fix, you'll get it automatically.`;
}

export async function runEapReleaseActions(version: string): Promise<void> {
  const ticketsToUpdate = await getTicketsByQuery(READY_TO_RELEASE_NOT_IN_EAP_QUERY);
  console.log(`Have to update the following tickets: ${JSON.stringify(ticketsToUpdate)}`);

  const comment = buildEapReleaseComment(version);
  for (const ticketId of ticketsToUpdate) {
    await setTag(ticketId, RELEASED_IN_EAP_TAG_ID);
    await addComment(ticketId, comment);
  }
}
