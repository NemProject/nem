# Documentation Guidelines

These are some guidelines for writing *technical documentation*.

The goal of technical docs is to *teach*: there is something *we* know and the *reader* does not, and needs to learn.
Therefore, tech docs need to be clear, unambiguous, and concise.
Compare with *marketing material* which has different goals and uses different techniques.

A good document structure helps the reader find what they need quickly without having to read too much.
That said, if understanding a document requires previous knowledge, you must always state so in the introduction and provide links.

**Always put yourself in the shoes of the reader.**

## General

* **Keep the scope of the document in mind**.

    A document should precisely fulfill its purpose, nothing more, nothing less.
    It is a common pitfall to end up going into rabbit holes and spending half a document explaining irrelevant details.

* **Keep the audience in mind**.

    Always think whether your intended audience will understand what you are writing.
    Do they have all the necessary context? Education? Data?

* **Try to write short sentences.**

    Avoid complex grammar, complex use of tenses, ambiguous pronouns and so on.
    A good guideline when it comes to technical writing is to aim for 20-30 words per sentence.
    Keeping sentences short should however never come at the expense of clarity, syntactic cues and important information.

* **Consistency is key**.

    Be consistent in your use of formatting, words and expressions, as it makes the text easier to understand.

* **USE A SPELL CHECKER**.

    Seriously, I’m ready to use physical violence to enforce this one.

* **Use a Markdown checker when writing Markdown**.

    It will get rid of the most common (and annoying) markdown issues, like trailing white space, unnecessary blank lines around blocks, etc.
    At some point this might even be enforced.

## Structure

* Document and section titles should follow the [Chicago Title Capitalization](https://en.wikipedia.org/wiki/Title_case#Chicago_Manual_of_Style) standard.
* Documents should start with a level one heading and should ideally be the same as the file name.
* Sections should be ordered hierarchically. Each document starts with a level one heading (`#`), which can contain one or more level two headings (`##`), which can contain one or more level threes (`###`) and so on.

    You cannot skip levels, e.g., you cannot add a level 6 right after the title because it looks nice *in a particular app*.

## Markdown Formatting

* Lists should use the `*` character rather than the `-` character, always start capitalized and end with a full stop.
* Paragraphs that include multiple sentences should have the sentences on separate lines, so that updating one sentence results in a clear diff where only one line changes.
* For long documents, it is good to have a table of contents at the end of the introduction of the level one heading section.
* Always specify the language for code blocks so that neither the syntax highlighter nor the text editor must guess.
    If no specific type makes sense, just use `text`.

## Additional Formatting and Macros

Some plugins enable additional formatting. On top of them, a few macros have been created to simplify repeated process
like tutorial steps and multi-language code snippets.

### Glossary Links

Define glossary terms using:

```markdown
category:glossary_term
:   Definition.
```

If no category is used (and no colon after it), the default category is used.
The default category can also be used explicitly by using `_`.

Link to glossary terms using `<category:glossary_term>` and you'll get a popup with the definition when hovering
over the term in the text.

Link to glossary terms in the default category using `<glossary_term:>`.

You can provide an alternate text instead of the glossary term using a pipe `|`:
`<category:glossary_term|alternate_text>`.
The glossary plugin takes care of plurals, though, so they don't typically require the alternate text.

Every API class and method defines a term, so they can be linked to using, for example: `<java:NemFacade>`.
The available categories are `java`, `get`, `post`, `ser`, and `ws`.

### Tutorial Steps

These macros create a table with each row beginning with a big-numbered description and a floating screenshot on the right.
When clicked, the image is zoomed while the description is still shown.
Steps can be navigated while the image is zoomed.

```jinja
{% import 'tutorial.jinja2' as tutorial %}

{{ tutorial.list_begin() }}
{{ tutorial.step_begin("screenshots/create-profile-0.jpg") }}
Write here the description for this step.
{{ tutorial.step_end() }}
{{ tutorial.list_end() }}
```

[Usage example](./pages/en/userbook/wallet/create-profile.md).

Add as many `step_begin()` / `step_end()` pairs as required.

**Lists do not work correctly in the description**, because they are an HTML block element and do not flow around the floating picture.

### Multi-Language Code Snippets

These macros create a tab group with a code block and optional caption.

There are two versions:

The simplified one accepts a list of strings, describing the language and line range, and optionally a caption.

```jinja
{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_full("devbook/hello-world", ["py", "js"]) }}
{{ tutorial.code_snippet(["py:4:4", "js:4:4"])}}
{{ tutorial.code_snippet(["py:6:16", "js:6:16:The <js:TransferTransactionV1Descriptor> constructor only accepts parameters of the right type, \
making it easier to use during development. We can do almost any markdown here:\n
* One **black**\n
* Two"]) }}
```

The extended syntax accepts a list of objects, keyed by language code:

```jinja
{% import 'tutorial.jinja2' as tutorial with context %}

{{ tutorial.code_snippet({
  'py': { 'range': [41, 54] },
  'js': {
    'range': [40, 52],
    'descriptor': 'TransferTransactionV1Descriptor'
  }
}) }}
```

Available parameters are:

* `range`: List of two values indicating the start and end lines of the code snippet.
* `descriptor`: If present, includes an admonition about typed descriptors including a link to this descriptor.
* `caption`: Free text to add below the snippet.

`code_snippet` uses the filename of the previous `code_full`.

[Usage example](./pages/en/devbook/start/hello-world.md).

`code_full` inserts the whole source file, for all the listed languages, and sets the file name to be used by the snippet macros.
Each language tab can have an optional caption, separated from the language code by a colon.

`code_snippet` inserts a range of lines, with an optional caption.

**Captions allow complex markdown like lists and term links, but they are formatted differently.**
Lines must be continued by escaping the line break, and line breaks are inserted with \n.
See the example above.

The only supported language is Java (`java`).
See [`tutorial.jinja2`](./templates/macros/tutorial.jinja2) for details.

## Technical Writing

* Use American English (`organize` instead of `organise`, `behavior` instead of `behaviour`, etc.)
* Use the American format for dates with long month names: `January 9, 2023`. 3-letter short month names can be used when space is at a premium, for example on narrow table columns. In this case, use the Day-Month-Year format: `9-Jan-2023`.
* Do not use gendered pronouns when talking about users/consumers/whatever but always `they/their` instead.
* Avoid talking about `us`, or `we`, even if it means resorting to passive voice.
* Use active voice when there is no specific need to use passive.
* Do not use the future tense but use present simple for expressing general truths instead.
* Abbreviations and acronyms should be spelled out the first time they appear in any technical document with the shortened form appearing in parentheses immediately after the term.
    The abbreviation or acronym can then be used throughout the document.
* Avoid ambiguous and abstract language (e.g. `really`, `quite`, `very`), imprecise or subjective terms (i.e. `fast`, `slow`, `tall`, `small`) and words that have no precise meaning (i.e. `a bit`, `thing`, `stuff`).
* Avoid contractions (e.g. `don't`, `you'll`, etc.) as they are meant for informal contexts.
* Avoid generalized statements, because they are difficult to substantiate and too broad to be supported.
* Avoid story-telling, remain factual and concise.
* Avoid jargon.
* Humor is allowed, as long as it is not distracting. I.e., do not go out of your way for the sake of a pun.
* Avoid em-dashes `—`. Putting non-restrictive relative clauses into separate sentences leads to simpler, clearer writing.
    If em-dashes are needed, make sure to use the right character: `—` (alt code: `ALT+0151`).

    Most of the time what you really want is a colon `:`.
* When referring to something in a certain way (i.e. `FBAS` for *Federated Byzantine Agreement System*) make sure to consistently use only FBAS after the term is introduced.
* Use digits when the number is mostly meant to be used in a program.
    Spell out numbers when they are not (e.g., when a number can be a pronoun, such as in *that's the one I used*).

## Links

* Use informative link titles.
    For example, instead of naming your links `link` or `here`, wrap part of the sentence that is meant to be linked as a title.
* Links to external sources should be:
    * Clear, concise, factual (not tips & tricks-type articles, or blog posts).
    * Reliable to stand the test of time (will not start to 404 because it's a personal blog and the person decided to get rid of it, for example).
    * From reliable sources (this is where Wikipedia isn't always perfect, but fine for technical subjects).
* Whenever possible, use internal links instead of external ones: if something has been described in our documents somewhere, link to it instead of externally.

## Official Spellings

* dapp
* mainnet (or main network)
* smart contracts
* testnet (or test network)
* web3
