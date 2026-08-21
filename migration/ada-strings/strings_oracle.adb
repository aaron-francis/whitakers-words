-- Reference implementation (oracle) for the migration of
-- Latin_Utils.Strings_Package.Lower_Case / Upper_Case / Trim / Head.
--
-- Conforms to migration/CONTRACT.md: reads one input per line from stdin,
-- writes exactly one "<input><TAB><result>" line per input to stdout.
--
-- The legacy source is used unmodified; this program only calls it.

with Ada.Command_Line;
with Ada.Strings.Unbounded;   use Ada.Strings.Unbounded;
with Ada.Text_IO;             use Ada.Text_IO;
with Latin_Utils.Strings_Package;

procedure Strings_Oracle is

   package SP renames Latin_Utils.Strings_Package;

   Error_Result : constant String := "!ERROR";
   Max_Count    : constant Natural := 100_000;

   --  Escape decoding: see migration/FORMAT-strings.md
   procedure Decode
      (Text : in     String;
       Out_S :   out Unbounded_String;
       Ok    :   out Boolean)
   is
      I : Positive := Text'First;
   begin
      Out_S := Null_Unbounded_String;
      Ok    := True;
      while I <= Text'Last loop
         if Text (I) = '\' then
            if I = Text'Last then
               Ok := False;
               return;
            end if;
            case Text (I + 1) is
               when '\'    => Append (Out_S, '\');
               when '|'    => Append (Out_S, '|');
               when 's'    => Append (Out_S, ' ');
               when 't'    => Append (Out_S, Character'Val (9));
               when 'n'    => Append (Out_S, Character'Val (10));
               when 'v'    => Append (Out_S, Character'Val (11));
               when 'f'    => Append (Out_S, Character'Val (12));
               when 'r'    => Append (Out_S, Character'Val (13));
               when others =>
                  Ok := False;
                  return;
            end case;
            I := I + 2;
         elsif Text (I) = ' ' or else Text (I) = Character'Val (9) then
            --  Raw whitespace is not a legal encoding.
            Ok := False;
            return;
         else
            Append (Out_S, Text (I));
            I := I + 1;
         end if;
      end loop;
   end Decode;

   function Encode (S : String) return String is
      Result : Unbounded_String;
   begin
      for I in S'Range loop
         case S (I) is
            when '\'               => Append (Result, "\\");
            when '|'               => Append (Result, "\|");
            when ' '               => Append (Result, "\s");
            when Character'Val (9) => Append (Result, "\t");
            when Character'Val (10) => Append (Result, "\n");
            when Character'Val (11) => Append (Result, "\v");
            when Character'Val (12) => Append (Result, "\f");
            when Character'Val (13) => Append (Result, "\r");
            when others            => Append (Result, S (I));
         end case;
      end loop;
      return To_String (Result);
   end Encode;

   --  Split a line into at most three '|'-separated fields. Encoded fields
   --  never contain a raw '|', so splitting is unambiguous.
   procedure Split
      (Line   : in     String;
       F1, F2, F3 :   out Unbounded_String;
       N      :   out Natural)
   is
      Current : Unbounded_String := Null_Unbounded_String;
      Fields  : array (1 .. 4) of Unbounded_String
         := (others => Null_Unbounded_String);
      Count   : Natural := 1;
   begin
      for I in Line'Range loop
         if Line (I) = '|' then
            Fields (Count) := Current;
            Current := Null_Unbounded_String;
            Count := Count + 1;
            if Count > Fields'Last then
               --  More than three fields: report as such (caller errors out).
               F1 := Fields (1);
               F2 := Fields (2);
               F3 := Fields (3);
               N  := Fields'Last;
               return;
            end if;
         else
            Append (Current, Line (I));
         end if;
      end loop;
      Fields (Count) := Current;
      F1 := Fields (1);
      F2 := Fields (2);
      F3 := Fields (3);
      N  := Count;
   end Split;

   function Parse_Count (Text : String; Ok : out Boolean) return Natural is
      Value : Natural := 0;
   begin
      Ok := Text'Length > 0;
      if not Ok then
         return 0;
      end if;
      for I in Text'Range loop
         if Text (I) not in '0' .. '9' then
            Ok := False;
            return 0;
         end if;
         Value := Value * 10 + (Character'Pos (Text (I)) - Character'Pos ('0'));
         if Value > Max_Count then
            Ok := False;
            return 0;
         end if;
      end loop;
      return Value;
   end Parse_Count;

   function Evaluate (Line : String) return String is
      F1, F2, F3 : Unbounded_String;
      N          : Natural;
      Arg        : Unbounded_String;
      Ok         : Boolean;
   begin
      Split (Line, F1, F2, F3, N);
      if N < 2 then
         return Error_Result;
      end if;

      Decode (To_String (F2), Arg, Ok);
      if not Ok then
         return Error_Result;
      end if;

      declare
         Symbol : constant String := To_String (F1);
         Source : constant String := To_String (Arg);
         Extra  : constant String := To_String (F3);
      begin
         if Symbol = "Lower_Case" then
            if N /= 2 then
               return Error_Result;
            end if;
            return Encode (SP.Lower_Case (Source));

         elsif Symbol = "Upper_Case" then
            if N /= 2 then
               return Error_Result;
            end if;
            return Encode (SP.Upper_Case (Source));

         elsif Symbol = "Trim" then
            if N = 2 then
               return Encode (SP.Trim (Source));            --  default: Both
            elsif N = 3 then
               if Extra = "Left" then
                  return Encode (SP.Trim (Source, SP.Left));
               elsif Extra = "Right" then
                  return Encode (SP.Trim (Source, SP.Right));
               elsif Extra = "Both" then
                  return Encode (SP.Trim (Source, SP.Both));
               else
                  return Error_Result;
               end if;
            else
               return Error_Result;
            end if;

         elsif Symbol = "Head" then
            if N /= 3 then
               return Error_Result;
            end if;
            declare
               Count_Ok : Boolean;
               Count    : constant Natural := Parse_Count (Extra, Count_Ok);
            begin
               if not Count_Ok then
                  return Error_Result;
               end if;
               return Encode (SP.Head (Source, Count));
            end;

         else
            return Error_Result;
         end if;
      end;
   end Evaluate;

begin
   while not End_Of_File (Standard_Input) loop
      declare
         Line : constant String := Get_Line (Standard_Input);
      begin
         Put_Line (Line & Character'Val (9) & Evaluate (Line));
      end;
   end loop;
   Ada.Command_Line.Set_Exit_Status (Ada.Command_Line.Success);
end Strings_Oracle;
