create or replace package somepackage is

a varchar2(200);
a2 varchar2(200
);
b varchar2(200 char);
c varchar2(200 byte);

subtype bla1 is varchar2(400 char);
subtype bla2 is varchar2(400/*bla*/);

type r is record (a varchar2(20),
a varchar2(20 char),
a varchar2(20 byte));

type ar is table of varchar2(20);
type ar is table of varchar2(20 char);

type ar2 is table of varchar2(20 char) index by binary_integer;




procedure p(x varchar2);
end;

end;