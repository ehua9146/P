﻿using System;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;

namespace Microsoft.Pc
{
    public static class CommandLineOptions
    {
        private static readonly Lazy<bool> isFileSystemCaseInsensitive = new Lazy<bool>(() =>
        {
            var file = Path.GetTempPath() + Guid.NewGuid().ToString().ToLower() + "-lower";
            File.CreateText(file).Close();
            var isCaseInsensitive = File.Exists(file.ToUpper());
            File.Delete(file);
            return isCaseInsensitive;
        });

        private static bool IsFileSystemCaseInsensitive => isFileSystemCaseInsensitive.Value;

        private static readonly DefaultCompilerOutput CommandlineOutput = new DefaultCompilerOutput(new DirectoryInfo(Directory.GetCurrentDirectory()));
        public static bool ParseArguments(IEnumerable<string> args, out CompilationJob job)
        {
            job = null;

            var outputLanguage = CompilerOutput.C;
            DirectoryInfo outputDirectory = null;
            
            var commandLineFileNames = new List<string>();
            var inputFiles = new List<FileInfo>();
            string targetName = null;

            foreach (var x in args)
            {
                var arg = x;
                string colonArg = null;
                if (arg[0] == '-')
                {
                    var colonIndex = arg.IndexOf(':');
                    if (colonIndex >= 0)
                    {
                        arg = x.Substring(0, colonIndex);
                        colonArg = x.Substring(colonIndex + 1);
                    }

                    switch (arg.Substring(1).ToLowerInvariant())
                    {
                        case "t":
                        case "target":
                            if (colonArg == null)
                            {
                                CommandlineOutput.WriteMessage("Missing target name", SeverityKind.Error);
                            }
                            else if (targetName == null)
                            {
                                targetName = colonArg;
                            }
                            else
                            {
                                CommandlineOutput.WriteMessage("Only one target must be specified", SeverityKind.Error);
                            }

                            break;

                        case "g":
                        case "generate":
                            switch (colonArg?.ToLowerInvariant())
                            {
                                case null:
                                    CommandlineOutput.WriteMessage("Missing generation argument, expecting generate:[C,P#]", SeverityKind.Error);
                                    return false;
                                case "c":
                                    outputLanguage = CompilerOutput.C;
                                    break;
                                case "p#":
                                    outputLanguage = CompilerOutput.PSharp;
                                    break;
                                default:
                                    CommandlineOutput.WriteMessage($"Unrecognized generate option '{colonArg}', expecting C or P#", SeverityKind.Error);
                                    return false;
                            }

                            break;

                        case "o":
                        case "outputdir":
                            if (colonArg == null)
                            {
                                CommandlineOutput.WriteMessage("Must supply path for output directory", SeverityKind.Error);
                                return false;
                            }

                            outputDirectory = Directory.CreateDirectory(colonArg);
                            break;

                        default:
                            commandLineFileNames.Add(arg);
                            CommandlineOutput.WriteMessage($"Unknown Command {arg.Substring(1)}", SeverityKind.Error);
                            return false;
                    }
                }
                else
                {
                    commandLineFileNames.Add(arg);
                }
            }

            // Each command line file name must be a legal P file name
            foreach (var inputFileName in commandLineFileNames)
            {
                if (IsLegalPFile(inputFileName, out FileInfo fullPathName))
                {
                    inputFiles.Add(fullPathName);
                }
                else
                {
                    CommandlineOutput.WriteMessage($"Illegal P file name {fullPathName} or file {fullPathName} not found", SeverityKind.Error);
                }
            }

            if (inputFiles.Count == 0)
            {
                CommandlineOutput.WriteMessage("At least one .p file must be provided", SeverityKind.Error);
                return false;
            }

            string projectName = targetName ?? Path.GetFileNameWithoutExtension(inputFiles[0].FullName);
            if (!IsLegalUnitName(projectName))
            {
                CommandlineOutput.WriteMessage($"{projectName} is not a legal protject name", SeverityKind.Error);
                return false;
            }


            if (outputDirectory == null)
            {
                outputDirectory = new DirectoryInfo(Directory.GetCurrentDirectory());
            }

            job = new CompilationJob(new DefaultCompilerOutput(outputDirectory), outputLanguage, inputFiles,
                projectName);
            return true;
        }

        private static bool IsLegalUnitName(string unitFileName)
        {
            return Regex.IsMatch(unitFileName, "^[A-Za-z_][A-Za-z_0-9]*$");
        }

        private static bool IsLegalPFile(string fileName, out FileInfo file)
        {
            file = null;
            if (fileName.Length <= 2 || !fileName.EndsWith(".p") || !File.Exists(Path.GetFullPath(fileName)))
            {
                return false;
            }

            var path = Path.GetFullPath(fileName);
            if (IsFileSystemCaseInsensitive)
            {
                path = path.ToLowerInvariant();
            }

            file = new FileInfo(path);

            return true;
        }

        public static void PrintUsage()
        {
            CommandlineOutput.WriteMessage("USAGE: Pc.exe file1.p [file2.p ...] [-t:tfile] [options]", SeverityKind.Info);
            CommandlineOutput.WriteMessage("-t:tfile           -- name of output file produced for this compilation unit; if not supplied then file1", SeverityKind.Info);
            CommandlineOutput.WriteMessage("-outputDir:path    -- where to write the generated files", SeverityKind.Info);
            CommandlineOutput.WriteMessage("-generate:[C,P#]", SeverityKind.Info);
            CommandlineOutput.WriteMessage("    C   : generate C", SeverityKind.Info);
            CommandlineOutput.WriteMessage("    P#  : generate P#", SeverityKind.Info);
        }
    }
}