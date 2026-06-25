#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: convert-apache-license-headers.sh [--check] [path...]

Convert top-of-file Apache 2.0 headers in .java files to the Broadcom short form.

Options:
  --check   Report files that would be changed, but do not modify them.

If no paths are given, the current directory is scanned.
USAGE
}

mode="write"
paths=()

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --check)
      mode="check"
      shift
      ;;
    --)
      shift
      while [[ "$#" -gt 0 ]]; do
        paths+=("$1")
        shift
      done
      ;;
    *)
      paths+=("$1")
      shift
      ;;
  esac
done

if [[ "${#paths[@]}" -eq 0 ]]; then
  paths=(.)
fi

changed=0
checked=0

while IFS= read -r -d '' file; do
  checked=$((checked + 1))
  if perl -0777 -e '
    use strict;
    use warnings;

    my $file = shift @ARGV;
    my $mode = shift @ARGV;

    open my $in, "<", $file or die "open $file: $!";
    local $/;
    my $content = <$in>;
    close $in;

    my $original = $content;

    my $changed = 0;
    if ($content =~ /\A(\/\*.*?\*\/\n?)/s) {
      my $header = $1;
      if ($header =~ /Licensed under the Apache License, Version 2\.0 \(the "License"\);/
          && $header =~ /^[ \t]*\*\s+Copyright\s+([^\n]*)$/m) {
        my $copyright_tail = $1;
        my ($dates) = ($copyright_tail =~ /^(.*?)\s+the original author or authors\b/);
        if (!defined $dates || $dates eq q{}) {
          ($dates) = ($copyright_tail =~ /^([0-9][0-9, \-]*(?:present)?)/);
        }
        if (defined $dates) {
          $dates =~ s/\s+$//;
        }
        my ($broadcom_year) = (defined $dates ? ($dates =~ /(\d{4})/) : ());
        if (defined $dates && $dates ne q{} && defined $broadcom_year && $broadcom_year ne q{}) {
          my $replacement = "/*\n"
            . " * Copyright $broadcom_year Broadcom Inc. and/or its subsidiaries. All Rights Reserved.\n"
            . " * Copyright $broadcom_year-present the original author or authors.\n"
            . " */\n";
          $content =~ s/\A\Q$header\E/$replacement/;
          $changed = 1;
        }
      }
    }

    if ($changed && $content ne $original) {
      if ($mode eq q{write}) {
        open my $out, ">", $file or die "write $file: $!";
        print {$out} $content;
        close $out;
      }
      print "$file\n";
      exit 0;
    }

    exit 1;
  ' "$file" "$mode"; then
    changed=$((changed + 1))
  fi
done < <(find "${paths[@]}" -type f -name "*.java" -not -path "*/target/*" -not -path "*/build/*" -print0)

if [[ "$mode" == "check" ]]; then
  echo "Checked $checked Java files; $changed would change."
  if [[ "$changed" -gt 0 ]]; then
    exit 1
  fi
  exit 0
fi

echo "Checked $checked Java files; converted $changed."