nhtml
=====

NHTML is a normalization of documents from {PDF, HTML, XML, SVG, PNG/image} into a single semantic format.

Documents come in several syntactic forms, usually with implied structure and semantics. This is difficult for machines to 
consume for repurposing or mining. HTML was developed as a simple but universal approach to managing document structure and semantics
and, when used with community agreement, is adequate for almost all purposes.

NHTML is a subset of standard XHTML5 confined to static tags (i.e. document structure rather than interactive behaviour). Most of these 
are structural with a few such as ```title``` being semantic. Semantics can be added through attributes (e.g. ```@class```) 
or RDFa. Where possible we shall use community approaches, such as the sub-headings developed by Senay Kafkas at EBI.

The normalization includes both flattening existing HTML into XHTML5 and also adding community-agreed tags for sections.

This project defines an emerging de facto approach, combined with (initially) Java code to convert raw documents into normalized XHTML5.
This conversion may be inevitably lossy where the input is designed only for human eyes (especially PDF and Images). It is likely to be
one-way - i.e. downstream enrichment may not be layerable on the initial (PDF) document; we shall keep all backwards mappings open 
as far as possible.

The primary current driver is mining the 1.5 million scholarly articles per year, but the technology should apply well to government
and similar outputs. There are deliberately no attempts to restrict access to intermediate components or outputs so users of confidential material should be
aware of this.

The initial development will take place on Bitbucket (http://bitbucket.org/petermr) as the rest of the Java code is there. We expect to port to Github when it makes sense.
