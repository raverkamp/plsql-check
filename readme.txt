Varchar2 Fixer

This program fixes declarations of kind varchar2(23) to varchar2(23 char).
Of course char() and varchar() are also handled.

Call 
dir <directoryname>

fixes all files in dirextory and replaces the files in place.
You art using source control?


Given a file name, detremine what the contents are: spec or body and then
retrive all varchar() declarations. 
These can occur as 
  variable type,
  record field,
  table of something

Of course it would be simpler to do this with a regular expression.

But this is a test for the pl/sql parser.

And now I have a simple CodeWalker.

