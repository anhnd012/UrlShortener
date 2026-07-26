# Project Lynk — Eight-Week Product Engineering Simulation

## 1. Your role

You are the first backend engineer at a small startup. You do not receive perfectly written tickets. A founder brings customer complaints, sales promises, incidents, partial metrics, and deadlines. Your job is to discover the real requirement, protect the product, choose a design, ship the smallest valuable change, and explain the consequences.

You are not being trained to memorize Redis, Kafka, Docker, or AWS. You are being trained to recognize the underlying problem—caching, asynchronous delivery, isolation, resource limits, failure recovery, or data ownership—and then select an implementation deliberately.

For this simulation:

- you own product clarification as well as backend implementation;
- unanswered questions are part of the exercise, not mistakes in the document;
- a technically impressive feature that customers do not need is failure;
- a simple design with clear limits is preferred over architecture theatre;
- the mentor will not reveal the hidden requirement until you have stated your assumptions;
- code is only one part of the submission.

## Senior-behavior contract for this simulation

Two months cannot manufacture professional seniority. A Senior Engineer has normally seen decisions survive multiple releases, real incidents, changing requirements, team turnover, and long-term maintenance. This project cannot reproduce that history or grant a job title.

It can force senior-level behaviors within one bounded system. By the final defense, you must be able to:

1. own an ambiguous customer outcome rather than wait for a complete ticket;
2. find missing requirements and state safe assumptions;
3. protect invariants across concurrency, retries, and partial failure;
4. choose the simplest architecture that meets current evidence;
5. reject attractive work that does not serve the product;
6. diagnose across application, database, messaging, network, and runtime layers;
7. lead an incident and verify data correctness after recovery;
8. communicate decisions so another engineer can operate and change the system;
9. revise a decision when evidence invalidates its assumptions;
10. accept delivery ownership without hiding quality or uncertainty.

Passing all tasks does not automatically pass the simulation. The final assessment must show these behaviors without reading a prepared solution.

If the final score is at least 90 with no automatic rejection, the result is recorded as:

```text
SENIOR BEHAVIOR SIMULATION PASSED — PROJECT LYNK SCOPE
```

This statement is deliberately narrower than “Senior Engineer.” The honest expected career outcome, depending on your starting point, is stronger fundamentals and credible strong-junior/mid-level evidence with selected senior behaviors.

What remains outside the two-month claim:

- multi-quarter ownership and maintenance consequences;
- technical leadership across several engineers or teams;
- prioritization across a portfolio of systems;
- repeated real production incidents and customer escalations;
- hiring, mentoring, delegation, and organizational influence;
- deep expertise across every listed backend domain.

## 2. Product mandate

The market already has many URL shorteners. “Make a URL shorter” is not a sufficient reason for another one to exist.

The working product thesis is:

> **A short link that recipients can trust and owners can rescue after it has already been shared.**

The initial customer hypothesis is a small team, creator, event organizer, or campaign owner who shares links in places that are expensive or impossible to edit later: printed QR codes, newsletters, social posts, presentation decks, partner pages, and customer messages.

The hidden pain is not the number of characters. It is loss of control after distribution:

- the destination breaks;
- the wrong link was published;
- a campaign expires at the wrong local time;
- a suspicious-looking short link reduces trust;
- traffic arrives but the owner cannot understand what changed;
- an incident happens during a launch and there is no safe fallback.

The product should eventually make these users feel:

```text
“I can publish this once, understand what happens, and recover if something goes wrong.”
```

Candidate differentiators include destination history and undo, link-health warnings, a safe fallback, recipient trust preview, launch readiness, and privacy-conscious insights. They are hypotheses, not guaranteed requirements. You must decide which deserve implementation.

## 3. Learning model

The roadmap uses a T-shaped model. Priority controls how deeply a subject is reviewed; it does not mean lower-priority concerns may be ignored when they affect customers.

### Priority 1 — Mandatory depth

These four domains receive most study and interview time.

| Domain | Depth expected during this project |
| --- | --- |
| Backend fundamentals | Java core/OOP, collections and generics, exceptions, streams and functional boundaries, JVM/memory/GC/class loading/JIT concepts, thread safety and concurrency, Spring DI/MVC/JPA/validation/configuration/bean lifecycle |
| Database engineering | relational modeling, PostgreSQL indexes and plans, transactions/MVCC/isolation/locks, pagination, ORM/persistence context/dirty checking/fetching/N+1, and cache-aside/TTL/failure patterns |
| Distributed systems | messaging abstractions, producers/consumers/groups/offsets, delivery and ordering, idempotency/retry/DLT, rate limiting/backpressure, partial failure, consistency/availability/partition trade-offs |
| Software design | responsibility and boundaries, SOLID/DRY/KISS/YAGNI, patterns only when earned, modular/layered architecture, and lightweight domain modeling |

Priority 1 is not a vocabulary exam. A topic counts only when it helps explain behavior, choose a design, diagnose a failure, or prove an invariant.

### Priority 2 — Applied supporting depth

- Performance engineering: pools, batching, streaming, memory, slow queries, contention, profiling, latency/throughput/saturation.
- Testing: JUnit/Mockito, integration and system tests, Testcontainers, happy paths, boundaries, failure, and concurrency.
- Product thinking: customer outcome, why a component exists, failure impact, cost, trade-off, and measurable success.

### Priority 3 — Working literacy

- Production engineering: metrics, logs, tracing concepts, alerts, incident response, and root-cause analysis.
- Security: authentication/authorization, injection, browser threats, abuse prevention, and rate limits.
- AI development workflow: use Codex or another coding assistant for critique, investigation, review, and controlled refactoring without outsourcing understanding.

You are not expected to master every named topic in two months. You are expected to transfer fundamentals to unfamiliar problems and explain what you still do not know.

## 4. Product and engineering constraints

These constraints remain true unless a later exercise changes them:

- Delivery window: July 20 through September 13, 2026.
- Team: one engineer and one strict mentor/reviewer.
- Budget: startup-level; avoid infrastructure that needs a dedicated platform team.
- Existing application: Java 21, Spring Boot, PostgreSQL, Flyway, Redis code, Docker Compose, tests, Actuator, and a basic k6 scenario.
- PostgreSQL is the durable source of truth unless an approved decision changes that.
- A redirect is customer-facing and must remain fast and correct during non-critical dependency failure.
- Personal data collection must have an explicit product purpose and retention policy.
- The system may begin as a modular monolith. Microservices require measured organizational or scaling pressure.
- Resume claims must be reproducible from committed evidence.

No traffic forecast, availability target, customer tier, privacy jurisdiction, abuse model, or analytics accuracy requirement is provided globally. Discover what matters for each problem.


## 5. How an exercise works

Each exercise is deliberately incomplete. Start from the live Linear issue, then create the Current Design in the Project Lynk Notion workspace. Reproduce every applicable section from `docs/templates/task-design-document.md`; do not begin with a repository draft. The design must contain:

1. the customer and business outcome;
2. questions that could materially change the design;
3. assumptions you propose if answers are unavailable;
4. invariants that must survive concurrency and failure;
5. the mandatory 1:3:1 decision: one problem, three genuinely viable solutions with trade-offs, and one selected solution;
6. failure and edge cases;
7. detailed flows, contracts, state/data, transaction, dependency, security, resource, and operational behavior where relevant;
8. how success and correctness will be demonstrated;
9. estimate and commit-sized delivery plan.

Attach the page to its Linear issue as `Design — Txx — Notion`, then run `review design Txx`. The mentor verifies the Notion design against the current repository before granting implementation permission. Questions that do not affect a decision may remain unanswered; proceed with a documented assumption. Missing or inaccessible design, straw-man alternatives, or unverified critical assumptions block implementation.

Approval is revision-specific. The mentor records the Notion page ID and `last_edited_time`, and the exact approved revision is immediately exported to `docs/designs/Txx-<name>.md`. Any later edit to the Notion design invalidates approval until the changed revision is reviewed again.

### Mandatory improvement workflow

The live lifecycle authority is the [Project Lynk workflow contract in Linear](https://linear.app/url-shortener-duc-anh/document/project-lynk-task-design-and-review-contract-636993dce288). `docs/workflow.md` is its repository mirror; when they conflict, report the drift and use Linear for lifecycle control until the mirror is repaired.

```text
start Txx
-> Steps 0–7: discovery, 1:3:1 decision, and detailed Notion design
-> review design Txx
-> Steps 8–10: implement the approved vertical slice; feature runs; smoke and all required checks are green
-> review implementation Txx
-> Steps 11–12: GitHub inline code review plus a durable Notion implementation-review page
-> Steps 13–15: comprehensive integration, failure, concurrency, real-infrastructure, and tool-evidence pass; rerun all tests and runtime smoke
-> review evidence Txx
-> Step 16: knowledge defense, learning record, retention schedule, and closure
-> close Txx
```

The comprehensive evidence pass happens after code review. However, implementation review is still blocked until the feature runs, the smoke flow succeeds, and all required repository checks are green.

Codex owns Linear status changes for `start`, every review command, and `close`; the learner does not infer or manually advance lifecycle state. The workflow contract also defines the blocked protocol, AI usage, evidence, deadline incidents, definition of done, and seven/twenty-one-day retention loop. Do not duplicate or override those rules in task notes.

An exercise is complete only when it has:

- an approved clarification/design note;
- a narrow implementation or a justified no-build decision;
- tests at appropriate boundaries;
- reproducible evidence;
- updated decision/risk records;
- a short product demo and architecture explanation;
- an updated learning-journal entry with a scheduled review.


## 6. Enforcement

- Work in progress limit: one exercise.
- Main build red for more than 24 hours: all product work freezes.
- Score below 80: submission is rejected and must be remediated.
- Missed deadline: write a delivery incident within 12 hours.
- Two consecutive misses: re-plan and cut optional scope; final deadline remains fixed.
- Missing evidence: incomplete.
- Unable to explain a decision or changed line: incomplete.
- Adding infrastructure without a problem statement and operational owner: rejected.
- Implementing a deliberately omitted requirement without asking or stating an assumption: penalized.
- Design work exceeds 60 minutes: reduce scope before adding ceremony.
- More than five important findings in one review: report the five highest-risk findings and defer lower-value polish.
- A third design-review round: reduce the proposed scope before continuing.
- An approved Notion design changes: implementation approval is invalid until the new revision is reviewed and exported.
- Closing a task without +7-day and +21-day Linear retention issues: incomplete.

Every Sunday gate includes three architecture questions and one unseen failure scenario. The mentor grades reasoning from evidence, not framework vocabulary.

## 7. Deadline calendar

Deadlines are in ICT. Linear is authoritative for live blockers, status, and due dates. On July 18, 2026, the owner explicitly moved T31 ahead of T08 so the essential customer workflow takes priority over optional scope selection; the former T08 → T31 blocker was removed. This changed execution priority, not the recorded deadlines: T08 remains due August 2 and T31 remains due August 4. After current prerequisite and WIP obligations, prioritize T31 rather than inferring execution order from dates or task numbers.

T17–T19 and T21 are deliberately pulled forward before the customer-facing reports T33–T34. The final defense remains fixed, so the remaining curriculum is compressed and several tasks have only one day. Any missed intermediate deadline still requires a delivery incident; do not silently move the baseline.

| Week | Exercise | Deadline | Product checkpoint |
| --- | --- | --- | --- |
| 1 | T01 | Mon, Jul 20 | Restore a trustworthy baseline |
| 1 | T02 | Wed, Jul 22 | Choose a customer problem |
| 1 | T03 | Fri, Jul 24 | Define link creation |
| 1 | T04 | Sun, Jul 26, 17:00 | Make redirect dependable |
| 2 | T05 | Mon, Jul 27 | Prevent unsafe use |
| 2 | T06 | Wed, Jul 29 | Define expiry behavior |
| 2 | T07 | Fri, Jul 31 | Establish ownership |
| 2 | T08 | Sun, Aug 2, 17:00 | Decide the MVP |
| Core | T31 | Tue, Aug 4 | Find owned campaign links |
| Core | T32 | Thu, Aug 6 | Stop or remove a link safely |
| Core | T17 | Sat, Aug 8 | Define ethical click data |
| Core | T18 | Mon, Aug 10 | Decouple analytics safely |
| Core | T19 | Wed, Aug 12 | Tolerate bad/duplicate events |
| Core | T21 | Fri, Aug 14 | Make report boundaries stable |
| Core | T33 | Sun, Aug 16, 17:00 | Show per-link performance over time |
| Core | T34 | Tue, Aug 18 | Show privacy-conscious geographic insight |
| 3 | T09 | Thu, Aug 20 | Survive short-code growth |
| 3 | T10 | Fri, Aug 21 | Make retries safe |
| 3 | T11 | Sat, Aug 22 | Offer memorable aliases |
| 3 | T12 | Sun, Aug 23, 17:00 | Add destination undo |
| 4 | T13 | Mon, Aug 24 | Detect broken destinations |
| 4 | T14 | Tue, Aug 25 | Rescue a live campaign |
| 4 | T15 | Wed, Aug 26 | Increase recipient trust |
| 4 | T16 | Thu, Aug 27, 17:00 | Build Launch Guard |
| 5 | T20 | Fri, Aug 28, 17:00 | Turn counts into decisions |
| 6 | T22 | Sun, Aug 30, 17:00 | Keep reports fast as data grows |
| 6 | T23 | Mon, Aug 31 | Give customers data control |
| 6 | T24 | Wed, Sep 2, 17:00 | Expire links safely at scale |
| 7 | T25 | Thu, Sep 3 | Define when the product is healthy |
| 7 | T26 | Sat, Sep 5 | Lead an incident |
| 7 | T27 | Mon, Sep 7 | Make deployment reproducible |
| 7 | T28 | Wed, Sep 9, 17:00 | Make unsafe releases difficult |
| 8 | T29 | Fri, Sep 11 | Validate product and capacity |
| 8 | T30 | Sun, Sep 13, 18:00 | Defend the launch |

Final architecture and product defense: **Sunday, September 13, 2026 at 20:00 ICT**.

## 8. Exercises

## Week 1 — Find the product and restore trust

### T01 — The build nobody trusts

It is your first morning. The previous engineer added a cache dependency and left. The test command no longer completes, and the founder wants a new feature by tomorrow. Some tests may be stale; some production behavior may also be wrong.

The company needs a trustworthy baseline before it can make promises. Determine what failed, what the failure prevents the team from knowing, and the smallest safe recovery plan. Do not hide a failure by deleting checks or lowering a gate.

Your submission must separate diagnosis from repair and show why the restored suite deserves trust.

### T02 — Who is this actually for?

The founder says, “Everyone shares links. Our market is everyone.” There is no customer research, pricing model, or proof that another generic shortener will be used. You have time for five short conversations or equivalent structured observations.

Choose one narrow early customer and one expensive moment in their workflow. Define the job they hire this product to do, what they use today, and why changing behavior would be worthwhile. Produce a product hypothesis that can be disproved within the eight-week window.

Do not solve this by listing features or copying a competitor.

### T03 — “Just let me create a link”

The first design partner wants to paste a destination and receive something shareable immediately. Marketing asks for optional expiry. Support asks for useful errors. The founder wants the endpoint public to reduce onboarding friction.

Define the smallest creation experience worth shipping. Several details are intentionally unspecified: identity, repeated requests, URL acceptance, default lifetime, response shape, and what “created” means if the client disconnects.

The result must be understandable to a customer and stable enough that a second client could integrate without reading the source.

### T04 — The launch-day redirect

A design partner prints 20,000 flyers. On launch morning, one supporting dependency becomes slow. Recipients do not care about the internal architecture; the printed link must either reach the correct destination or fail in a predictable, safe way.

Define the redirect contract and dependency policy. Identify which failures may be hidden, which must be surfaced, and what correctness means when stored state changes while faster copies still exist.

Demonstrate the important paths, including one degraded path, without relying only on mocked behavior.

**Gate G1:** defend the customer hypothesis, baseline recovery, creation contract, and redirect failure policy.

## Week 2 — Make trust a product property

### T05 — The link that should never have been created

Abuse reports arrive during a private beta. Some destinations are malformed, some hide credentials, some use unexpected schemes, and some point to addresses that only make sense inside a private network. A teammate uses “SSRF,” “open redirect,” and “phishing” as if they were the same problem.

Define what this product accepts, rejects, records, and explains to the customer. Your policy must be consistent enough to test, but it cannot claim protection the product does not actually provide.

Assume attackers can send deliberately confusing input.

### T06 — “Expires Friday”

An event organizer asks for a link to expire Friday night. Their team spans Bangkok, London, and New York. After expiry, support wants a helpful experience; security wants no information leakage; marketing wants the link reusable next month.

Define the lifecycle of a link and what each external observer sees. Decide what “Friday night” means, whether expiry is reversible, and whether an old short code can ever represent a new destination.

Demonstrate boundary behavior rather than only ordinary dates.

### T07 — The first shared workspace

Two people from the same customer need to manage campaign links. One leaves the company. Another guesses a management URL. The public redirect must remain frictionless, but changing a destination now has business impact.

Define ownership and authorization for creation, inspection, editing, disabling, and viewing analytics. The startup cannot build a complete enterprise identity platform this month.

Choose a small security boundary that can evolve without pretending the product is enterprise-ready.

### T08 — The founder wants six more features

After seeing the prototype, the founder requests QR images, branded domains, link-in-bio pages, webhooks, teams, and AI-generated campaign names. None has customer evidence. The beta date is unchanged.

Choose the smallest product slice for the next six weeks. State which promises are kept, postponed, or rejected and what evidence would change the decision. Include one differentiated capability tied to the customer problem from T02.

A large backlog is not a strategy.

**Gate G2:** defend the security/lifecycle model and the product scope you deliberately rejected.

## Core product essentials — priority path before T08 and the advanced curriculum

### T31 — Find the campaign link before the launch

A marketing manager has hundreds of links across clients and channels. Minutes before a campaign launch, they remember part of the landing-page address and the campaign name, but not the short code. The product can create another link, yet it cannot reliably show the links this owner already controls.

Create the smallest owner-facing link library that lets the customer find the correct link quickly. Search, filters, ordering, result size, empty states, ownership visibility, and behavior while links are created or changed are intentionally unspecified. The collection will not remain small, and returning everything is not an acceptable long-term contract.

A customer must be able to repeat the same query without links mysteriously moving, disappearing, or leaking from another owner.

### T32 — Stop the link without losing the truth

An affiliate discovers that a published offer is wrong and presses “delete” during a live campaign. Recipients may still be arriving, the link may be cached, analytics already exist, and support may need to explain what happened. The customer expects the link to stop causing harm immediately, but “delete” has no agreed meaning.

Define the customer-visible behavior for stopping, hiding, deleting, and possibly restoring an owned link. Decide what recipients, the owner's library, analytics, audit history, and future short-code creation observe. Two browser tabs or two team members may act at the same time, and a stale fast path may still know the previous state.

The product must not silently retarget an old public code or erase evidence merely because the interface used the word “delete.”

### T33 — Show whether this link is working

A campaign owner opens one short link after spending money across several channels. A lifetime total does not tell them whether traffic is growing, fading, or concentrated around a particular launch. They ask to inspect clicks by a chosen range and switch between useful day, month, and year views.

Create the smallest per-link performance experience that supports an actual campaign decision. Range limits, bucket boundaries, timezone, incomplete or delayed events, empty periods, comparison behavior, and what the API returns are intentionally unspecified. The same facts must not appear to change merely because a chart chooses a different interval.

The result must work through the customer interface and remain honest when analytics data is late or partially unavailable.

### T34 — Show where the campaign reached people

A marketing or affiliate customer buys traffic in several markets and asks which countries or regions actually responded to one short link. The available request data can suggest location, but travelers, VPNs, bots, carrier networks, missing data, and provider changes make the result uncertain.

Create the smallest geographic performance experience that can guide a campaign decision without implying that the product knows an individual's precise location. Collection timing, permitted granularity, unknown values, retention, historical backfill, small-result handling, ordering, and data freshness are intentionally unspecified.

The customer must be able to understand what the location numbers mean, what they do not mean, and why totals may not perfectly match another view.

**Core gate:** demonstrate that an owner can find, control, and measure one campaign link end to end. T31 is explicitly prioritized ahead of T08. T17–T19 and T21 are pulled forward as prerequisites for T33–T34. Live Linear blockers and recorded owner decisions—not numeric task order, document order, or due-date sorting—control execution.

## Week 3 — Protect correctness under growth and change

### T09 — The collision report

A dashboard shows two creation failures during a traffic spike. The team assumes short-code collision, but the same error category can also represent unrelated data violations. Sales asks whether eight characters support the next three years.

Determine what actually happened and define creation behavior under concurrent requests. Provide a capacity argument with explicit assumptions. A pre-insert existence check is proposed in chat as an easy fix.

Your result must not retry unrelated defects as if they were harmless collisions.

### T10 — The customer clicked Create twice

A mobile client timed out after sending a creation request. The user retried and received a second link. Finance now worries that future paid quotas will count both. Another engineer proposes deduplicating every identical destination forever.

Define the product's retry behavior without accidentally merging requests that the customer intended to keep separate. Address concurrent retries and a response lost after the server completed its work.

The client needs a rule it can use safely.

### T11 — The alias people remember

Customers ask for links such as `/summer-sale`. Two customers want the same alias. One enters mixed case; another uses a reserved word; a third tries to impersonate support. Marketing wants aliases to be reusable after deletion.

Define the namespace and lifecycle of memorable aliases. Consider what changes when branded domains arrive later, but do not build branded domains now unless the design requires it.

The public behavior must remain unambiguous under concurrency.

### T12 — Undo after publishing

A campaign manager edits the destination, then realizes the new page is wrong after thousands of messages have been sent. They ask support to “put it back exactly as it was ten minutes ago.” The current data model only remembers the latest destination.

Design an owner experience that makes a shared link recoverable without making redirects slow or allowing silent history rewriting. Decide what must be recorded, what can be restored, and what an audit should show.

Prove that a concurrent edit cannot produce an unexplained final state.

**Gate G3:** defend namespace, concurrency, retry, and destination-history decisions using database evidence.

## Week 4 — Create value users did not know to request

### T13 — “Why did clicks suddenly stop?”

A customer sees traffic fall to zero. The short link still redirects, but the destination now returns an error in one region. They expected the product to warn them before campaign money was wasted, although they never asked for monitoring.

Decide whether destination health belongs in the product. If it does, define a minimal signal useful enough to act on without turning the startup into a global monitoring company or creating a new abuse vector.

The check itself can fail, lie, or be blocked by the destination.

### T14 — Rescue mode

During a live launch, the primary destination is unavailable. The owner has a status page and an older landing page. They need a fast, safe action; recipients should not be trapped in a redirect loop or sent to an unapproved target.

Design a rescue capability that works under pressure. Clarify whether switching is manual or automatic, who can trigger it, how it is undone, and what happens to cached redirects.

The product must favor predictable control over clever automation.

### T15 — “I don't click unknown short links”

Interview participants say short links look suspicious because the destination is hidden. Owners want short links; recipients want evidence before trusting them. Showing the full destination can also reveal private campaign parameters.

Create a minimal trust experience for a cautious recipient. Decide what can be disclosed, what can be verified, and how to avoid presenting weak signals as a security guarantee.

Measure whether the experience improves understanding rather than merely adding a badge.

### T16 — Launch Guard

Customers currently discover mistakes after distribution. Before activating a high-value campaign, they want confidence that the destination, expiry, ownership, and fallback are sensible. They do not know which checks to ask for.

Design a preflight experience that finds preventable mistakes without blocking every unconventional but valid campaign. It must explain risk and allow an accountable decision.

Choose a few high-value checks. Do not build a generic rules engine.

**Gate G4:** demonstrate one latent-need feature with customer evidence and defend why the other ideas stayed small.

## Week 5 — Make analytics useful without making redirects fragile

### T17 — What is a click worth recording?

The founder asks for IP address, user agent, referrer, location, device, and “anything useful later.” A European beta customer asks how long data is retained and whether a recipient can be identified. The product hypothesis only requires campaign insight.

Define the smallest click record that serves an explicit customer decision. State what is not collected, how identifiers are handled, and when data disappears.

The contract must be able to evolve without silently changing the meaning of old events.

### T18 — Analytics must not break redirects

The analytics path becomes slow during a campaign. The founder accepts slightly delayed reports but not slower redirects. They have not said whether losing a small number of click events is acceptable.

Design the boundary between redirect and analytics. Identify the failure windows and force a product decision about delay, loss, duplication, and operational complexity.

Demonstrate behavior when the analytics dependency is unavailable and later recovers.

### T19 — The event that will not process

One analytics record repeatedly fails. Other records behind it stop appearing. After a restart, several clicks appear twice. Support needs reporting to recover without engineers manually editing the database.

Define processing behavior for duplicate, temporarily failing, permanently invalid, and unexpectedly old records. Preserve enough context to investigate without leaking unnecessary customer data.

Show that recovery does not corrupt the final result.

### T20 — “Total clicks” did not answer the question

A creator sees 8,421 clicks and asks, “Is that good? When did the campaign take off? Did the destination failure cost me traffic?” Raw counts do not help them decide what to do next.

Choose one decision-ready insight for the customer selected in T02. It must be explainable, privacy-conscious, and honest about missing or delayed data. Avoid a dashboard full of vanity metrics.

Validate comprehension with at least one person who did not implement it.

**Gate G5:** defend data minimization, delivery guarantees, recovery semantics, and the chosen customer insight.

## Week 6 — Make time and growth boring

### T21 — The report changed when the user traveled

A Bangkok campaign owner requests daily clicks while viewing the report in London. The totals shift between days. A campaign crossed a daylight-saving transition and one “day” did not contain 24 hours.

Define report boundaries and timezone behavior so repeated requests have a stable meaning. Address invalid zones, adjacent ranges, empty ranges, and late-arriving events.

The API must make inclusion rules unambiguous.

### T22 — The report that became a table scan

The beta dataset was fast. A realistic generated dataset makes the primary analytics request slow, and one proposed fix adds several indexes “just in case.” Write traffic is also increasing.

Diagnose the request with database evidence. Improve the important query without optimizing every possible query, and state the data volume at which the chosen design should be revisited.

The evidence must allow another engineer to reproduce the conclusion.

### T23 — “Delete my data”

A customer leaves and asks for their account, links, and analytics to be deleted. Finance wants aggregate business metrics. Security wants an audit trail. Support wants a reversible operation in case the request was accidental.

Define export, retention, deletion, and recovery boundaries. Identify which data belongs to the customer, which data the business may legitimately retain, and how asynchronous copies are handled.

Do not promise instantaneous deletion if the architecture cannot provide it.

### T24 — Two cleanup workers, one link

The application now runs more than one instance. Both find the same expired links. One worker crashes in the middle of a batch, and a large backlog causes resource saturation. Redirect correctness cannot wait for cleanup to catch up.

Design bounded, recoverable expiration processing. Show what happens with two workers and partial failure. Any concurrency mechanism must respect the capacity of the database and cache.

Technology choice must follow measurement, not resume value.

**Gate G6:** defend time boundaries, query plans, data lifecycle, transactions, locks, and measured concurrency.

## Week 7 — Operate and release the product

### T25 — Green health, broken product

The health endpoint says UP while customers cannot create links and analytics is hours behind. During a cache outage redirects still work but latency rises. The team debates whether every dependency should make the whole application unready.

Define product health from customer outcomes. Create signals that distinguish symptoms, causes, and acceptable degradation without producing an alert per short code or customer.

Someone unfamiliar with the code should know when to act.

### T26 — You are on call

The mentor will inject one failure without naming the component. You have ten minutes before opening source code and thirty minutes to restore an acceptable customer outcome. Data correctness matters as much as process availability.

Lead the incident, maintain a timeline, communicate impact, recover safely, and verify the result. Afterwards, document which assumption allowed the incident and one prevention action with an owner and deadline.

Speed without a reasoning trail does not pass.

### T27 — It only deploys from your laptop

The founder wants a beta environment. The repository contains local credentials and assumptions about ports and installed tools. The runtime image is larger than expected and starts with more privilege than necessary.

Make a clean environment able to build and start the supported product with documented configuration. Separate secret, environment-specific, and safe default values. Define startup, shutdown, migration, and health behavior.

The result must be inspectable and repeatable.

### T28 — Friday's unsafe release

A formatting problem, migration incompatibility, and vulnerable dependency are discovered after merge. The team wants automation, but a scanner with no triage policy could block every release.

Create a delivery gate that catches meaningful regressions on a clean runner. Decide which failures block release, which create warnings, and how an urgent fix can proceed without making bypass the normal workflow.

Demonstrate one intentionally failed change and its diagnosis before returning the pipeline to green.

**Gate G7:** diagnose an unseen incident and defend product health, deployment, migration, and release policy.

## Week 8 — Prove value, capacity, and judgment

### T29 — The beta decision

The founder asks whether to invite 100 more users. Existing load results only show a latency percentile with ten virtual users. Product interviews provide mixed feedback: users like rescue mode, but several never opened analytics.

Produce a go, limited-go, or no-go recommendation. Combine customer evidence, correctness, dependency degradation, resource saturation, and a reproducible workload. State the boundary beyond which your conclusion is invalid.

One measured product or performance improvement is required; vanity numbers are not.

### T30 — Defend what you built

An interviewer and an investor review the product. The interviewer cares about fundamentals and trade-offs. The investor cares about the customer problem, adoption signal, and why this product deserves to exist. Both will challenge claims that sound larger than the evidence.

Prepare a working demo, architecture view, decision history, failure story, capacity boundary, and product narrative. Explain what you deliberately did not build and the next three changes you would consider only after receiving new evidence.

Resume bullets may contain only facts another engineer can reproduce from the repository.

**Gate G8:** final product launch and architecture defense on September 13 at 20:00 ICT.

## 9. Weekly scoring

| Area | Points |
| --- | ---: |
| Problem discovery and product judgment | 20 |
| Correctness and fundamentals | 25 |
| Failure, concurrency, and data reasoning | 20 |
| Test/evidence quality | 15 |
| Architecture decisions and simplicity | 10 |
| Communication, ownership, and delivery | 10 |

Interpretation:

- below 70: major gaps; redo;
- 70–79: junior-level execution;
- 80–89: credible strong-junior/mid-level behavior;
- 90–100: senior-behavior simulation pass within Project Lynk's scope, not a Senior Engineer title.

Automatic rejection:

- implementation begins without questions or documented assumptions;
- main build or required tests remain red;
- only the happy path is demonstrated;
- infrastructure is added without an owned problem;
- secrets or unnecessary personal data are committed;
- a performance claim omits workload and errors;
- the owner cannot explain the behavior;
- the feature has no defined customer outcome.

## 10. Submission and approval format

Use the full template at `docs/templates/task-design-document.md` to create a Notion page titled:

```text
[Txx] <short name> — Detailed Design
```

Attach it to the matching Linear issue as `Design — Txx — Notion`. The Notion page is the Current Design during review; do not create a competing repository draft.

The required decision structure is:

```text
1 precisely scoped problem
-> 3 genuinely viable solutions with trade-offs
-> 1 selected solution with accepted risks and revisit trigger
```

Run `review design Txx`. Feature implementation remains locked until the verdict is `APPROVED_TO_IMPLEMENT`. Approval records the Notion page ID and `last_edited_time`; immediately export that exact approved revision to:

```text
docs/designs/Txx-<short-name>.md
```

Design-review rounds, implementation reviews, and evidence reviews are separate append-only Notion pages linked to the same Linear task. GitHub remains the inline code-review surface, but the complete implementation-review result must also be persisted in Notion.

The detailed recommended solution is intentionally separated into `docs/codex/recommended-solution.md`. It is a spoiler and mentor rubric. Do not read a task's solution before submitting your design document; doing so removes the product ambiguity this environment is designed to train.

## 11. Start or resume work

Never choose the next task from this static document alone. Run:

```text
start Txx
```

Codex must first query Linear, enforce WIP = 1, verify blockers and the current due date, and record the lifecycle transition. If a deadline has already passed, create the required delivery incident instead of silently editing the date.

For the current essential-first plan, T31 has priority over T08 once current prerequisite and WIP obligations permit it. The mentor grades how you reason from product and system evidence—not how quickly you make the build green.
