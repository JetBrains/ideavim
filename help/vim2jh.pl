#!/usr/bin/env perl

# converts vim documentation to simple html
# Sirtaj Singh Kang (taj@kde.org)
# Modified by Rick Maddy to generate Java Help map file too.

use strict;
use vars qw/%url $date/;

%url = ();
$date = `date`;
chop $date;

sub maplink
{
	my $tag = shift;
	if( exists $url{ $tag } ){
		return $url{ $tag };
	} else {
		print "Unknown hyperlink target: $tag\n";
		$tag =~ s/\.txt//;
		$tag =~ s/</&lt;/g;
		$tag =~ s/>/&gt;/g;
		return "<code class=\"badlink\">$tag</code>";
	}
}

sub readTagFile
{
    my($destdir) = shift;
	my($tagfile) = @_;
	my( $tag, $file, $name );

    mkdir($destdir);
	open(TAGS,"$tagfile") || die "can't read tags\n";
	open( OUT, ">$destdir/Map.jhm" )
			|| die "Couldn't write to $destdir/Map.jhm: $!.\n";

	print OUT<<EOF;
<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE map PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp Map Version 1.0//EN" "http://java.sun.com/products/javahelp/map_2_0.dtd">

<map version="1.0">
    <mapID target="vim.logo" url="images/vim16x16.gif"/>
    <mapID target="folder" url="images/folder.gif"/>
    <mapID target="doc" url="images/doc.gif"/>
EOF

	while( <TAGS> ) {
		next unless /^(\S+)\s+(\S+)\s+/;

		$tag = $1;
		my $label = $tag;
		($file= $2) =~ s/.txt$/.html/g;
#		$label =~ s/\.txt//;

		$url{ $tag } = "<a href=\"$file#vim.".escurl($tag)."\">".esctext($label)."</a>";
        print OUT "    <mapID target=\"vim.".escurl($tag)."\" url=\"vim/$file#vim.".escurl($tag)."\"/>\n";
	}
	close( TAGS );

    print OUT "</map>\n";
}

sub esctext
{
	my $text = shift;
	$text =~ s/&/&amp;/g;
	$text =~ s/</&lt;/g;
	$text =~ s/>/&gt;/g;
	return $text;
}

sub escurl
{
	my $url = shift;
	$url =~ s/%/%25/g;
	$url =~ s/"/%22/g;
	$url =~ s/~/%7E/g;
	$url =~ s/</%3C/g;
	$url =~ s/>/%3E/g;
	$url =~ s/=/%20/g;
	$url =~ s/#/%23/g;
	$url =~ s/&/%26/g;
	$url =~ s/\?/%3F/g;
	$url =~ s/\//%2F/g;
	$url =~ s/\./%2E/g;

	return $url;
}

sub vim2html
{
    my( $destdir ) = shift;
	my( $infile ) = @_;
	my( $outfile );

	open(IN, "$infile" ) || die "Couldn't read from $infile: $!.\n";

	($outfile = $infile) =~ s:.*/::g;
	$outfile =~ s/\.txt$//g;

    mkdir("$destdir/vim");
	open( OUT, ">$destdir/vim/$outfile.html" )
			|| die "Couldn't write to $destdir/vim/$outfile.html: $!.\n";
	my $head = uc( $outfile );

	print OUT<<EOF;
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<title>IdeaVIM Help: $outfile</title>
<link rel="stylesheet" href="vim-stylesheet.css" type="text/css">
</head>
<body>
<h2>$head</h2>
<pre>
EOF

	my $inexample = 0;
	my $inComment = 0;
	while( <IN> ) {
		chop;
		if ( /^<!--/ ) {
		    $inComment = 1;
		    next;
		}
		elsif ( /^-->/ ) {
		    $inComment = 0;
		    next;
		}
		elsif ( $inComment == 1 ) {
		    next;
        }
		elsif ( /^\s*[-=]+\s*$/ ) {
			print OUT "</pre><hr><pre>";
			#print OUT "<hr>";
			next;
		}
		# examples
		elsif( /^>$/ || /\s>$/ ) {
			$inexample = 1;
			chop;
		}
		elsif ( $inexample && /^([<\S])/ ) {
			$inexample = 0;
			$_ = $' if $1 eq "<";
		}

		s/\s+$//g;

		# Various vim highlights. note that < and > have already been escaped
		# so that HTML doesn't get screwed up.

		my @out = ();
		#		print "Text: $_\n";
		LOOP:
		foreach my $token ( split /((?:\|[^\|]+\|)|(?:\*[^\*]+\*))/ ) {
			if ( $token =~ /^\|([^\|]+)\|/ ) {
				# link
				push( @out, "|".maplink( $1 )."|" );
				next LOOP;
			}
			elsif ( $token =~ /^\*([^\*]+)\*/ ) {
				# target
				push( @out,
					"<b class=\"vimtag\">\*<a name=\"vim.".escurl($1)."\">".esctext($1)."<\/a>\*<\/b>");
				next LOOP;
			}

			$_ = esctext($token);
			s/CTRL-(\w+)/<code class="keystroke">CTRL-$1<\/code>/g;
			# parameter <...>
			s/&lt;(.*?)&gt;/<code class="special">&lt;$1&gt;<\/code>/g;

			# parameter {...}
			s/\{([^}]*)\}/<code class="special">{$1}<\/code>/g;

			# parameter [...]
			s/\[(range|line|count|offset|cmd|[-+]?num)\]/<code class="special">\[$1\]<\/code>/g;
			# note
			s/(Note:?)/<code class="note">$1<\/code>/gi;

			# local heading
			s/^(.*)\~$/<code class="section">$1<\/code>/g;
			push( @out, $_ );
		}

		$_ = join( "", @out );

		if( $inexample == 2 ) {
			print OUT "<code class=\"example\">$_</code>\n";
		} else {
			print OUT $_,"\n";
		}

		$inexample = 2 if $inexample == 1;
	}
	print OUT<<EOF;
</pre>
</body>
</html>
EOF

}

sub usage
{
die<<EOF;
vim2jh.pl: converts vim documentation to HTML.
usage:

	vim2jh.pl <dest dir> <tag file> <text files>
EOF
}



sub writeCSS
{
    my( $destdir ) = shift;
	open( CSS, ">$destdir/vim/vim-stylesheet.css"  ) || die "Couldn't write stylesheet: $!\n";
	print CSS<<EOF;
body { background-color: white; color: black;}
:link { color: rgb(0,137,139); }
:visited { color: rgb(0,100,100);
           background-color: white; /* should be inherit */ }
:active { color: rgb(0,200,200);
          background-color: white; /* should be inherit */ }

B.vimtag { color : rgb(250,0,250); }

h1, h2 { color: rgb(82,80,82); text-align: center; }
h3, h4, h5, h6 { color: rgb(82,80,82); }
.headline { color: rgb(0,137,139); }
.header { color: rgb(164, 32, 246); }
.section { color: rgb(164, 32, 246); }
.keystroke { color: rgb(106, 89, 205); }
.vim { }
.example { color: rgb(0, 0, 255); }
.option { }
.notvi { }
.special { color: rgb(106, 89, 205); }
.note { color: blue; background-color: yellow; }
.sub {}
.badlink { color: rgb(0,37,39); }
EOF

}

# main
usage() if $#ARGV < 2;

print "Processing tags...\n";
readTagFile( $ARGV[ 0 ], $ARGV[ 1 ] );

foreach my $file ( 2..$#ARGV ) {
	print "Processing ".$ARGV[ $file ]."...\n";
	vim2html( $ARGV[ 0 ], $ARGV[ $file ] );
}
print "Writing stylesheet...\n";
writeCSS( $ARGV[ 0 ] );
print "done.\n"
