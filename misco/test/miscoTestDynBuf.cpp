/*******************************************************************************
 * JMMC project
 *
 * "@(#) $Id: miscoTestDynBuf.cpp,v 1.3 2005-02-22 15:10:53 lafrasse Exp $"
 *
 * History
 * -------
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2005/02/11 11:04:38  lafrasse
 * Added miscoDYN_BUF
 *
 * Revision 1.1  2005/02/11 09:39:57  gzins
 * Created
 *
 ******************************************************************************/

/**
 * \file
 * miscoDYN_BUF test program.
 *
 * \synopsis
 * \<miscOTestDynBuf\>
 */

static char *rcsId="@(#) $Id: miscoTestDynBuf.cpp,v 1.3 2005-02-22 15:10:53 lafrasse Exp $"; 
static void *use_rcsId = ((void)&use_rcsId,(void *) &rcsId);


/* 
 * System Headers 
 */
#include <stdio.h>
#include <iostream>

/**
 * \namespace std
 * Export standard iostream objects (cin, cout,...).
 */
using namespace std;


/*
 * MCS Headers 
 */
#include "mcs.h"
#include "log.h"
#include "err.h"


/*
 * Local Headers 
 */
#include "misco.h"
#include "miscoPrivate.h"


/* 
 * Local functions  
 */
void displayExecStatus(mcsCOMPL_STAT);



/* 
 * Main
 */

int main(int argc, char *argv[])
{
    // Initialize MCS services
    if (mcsInit(argv[0]) == mcsFAILURE)
    {
        // Exit from the application with mcsFAILURE
        exit (EXIT_FAILURE);
    }

    miscoDYN_BUF   buffer;
    mcsCOMPL_STAT  executionStatusCode;

    char           *bytes       = NULL;
    int            bytesNumber  = 0;

    char           byte         = '\0';

    mcsUINT32      storedBytes  = 0;

    mcsUINT32      position     = 0;
    mcsUINT32      from         = 0;
    mcsUINT32      to           = 0;



    // buffer.AppendBytes
    cout << "---------------------------------------------------------" << endl;
    cout << "buffer.AppendBytes(NULL, 0) ";
    executionStatusCode = buffer.AppendBytes(bytes, bytesNumber);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytes = "hello buffer" ;
    bytesNumber = strlen(bytes);
    cout << "buffer.AppendBytes('" << bytes << "', " << bytesNumber << ") ";
    executionStatusCode = buffer.AppendBytes(bytes, bytesNumber);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;

    bytes    = " ... :)" ;
    bytesNumber = strlen(bytes);
    cout << "buffer.AppendBytes('" << bytes << "', " << bytesNumber << ") ";
    executionStatusCode = buffer.AppendBytes(bytes, bytesNumber);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;

    bytes    = " !!!" ;
    bytesNumber = strlen(bytes);
    cout << "buffer.AppendBytes('" << bytes << "', " << bytesNumber << ") ";
    executionStatusCode = buffer.AppendBytes(bytes, bytesNumber);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;



    // buffer.GetByteAt
    cout << "---------------------------------------------------------" << endl;
    position = miscDYN_BUF_BEGINNING_POSITION - 1;
    executionStatusCode = buffer.GetByteAt(NULL, position);
    cout << "buffer.GetByteAt(NULL, " << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    executionStatusCode = buffer.GetByteAt(&byte, position);
    cout << "buffer.GetByteAt(" << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    position = miscDYN_BUF_BEGINNING_POSITION;
    executionStatusCode = buffer.GetByteAt(&byte, position);
    cout << "buffer.GetByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    position = 7;
    executionStatusCode = buffer.GetByteAt(&byte, position);
    cout << "buffer.GetByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    executionStatusCode = buffer.GetByteAt(&byte, position);
    cout << "buffer.GetByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    position += 1;
    executionStatusCode = buffer.GetByteAt(&byte, position);
    cout << "buffer.GetByteAt(" << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.GetBytesFromTo
    cout << "---------------------------------------------------------" << endl;
    from = miscDYN_BUF_BEGINNING_POSITION - 1;
    to   = 4;
    buffer.GetNbStoredBytes(&storedBytes);
    bytes = (char*)calloc(sizeof(char), storedBytes + 1);
    bytes[0] = '\0';
    executionStatusCode = buffer.GetBytesFromTo(bytes, from, to);
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    from = miscDYN_BUF_BEGINNING_POSITION;
    to   = 12;
    executionStatusCode = buffer.GetBytesFromTo(NULL, from, to);
    cout << "buffer.GetBytesFromTo(NULL, " << from << ", " << to << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    executionStatusCode = buffer.GetBytesFromTo(bytes, from, to);
    bytes[(to - from) + 1] = '\0';
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") = '" << bytes
         << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    bytes[0] = '\0';
    cout << endl;

    from = 7;
    to   = 16;
    bytes[(to - from) + 1] = '\0';
    executionStatusCode = buffer.GetBytesFromTo(bytes, from, to);
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") = '" << bytes
         << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    bytes[0] = '\0';
    cout << endl;

    from = 6;
    to   = 6;
    bytes[(to - from) + 1] = '\0';
    executionStatusCode = buffer.GetBytesFromTo(bytes, from, to);
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") = '" << bytes
         << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    bytes[0] = '\0';
    cout << endl;

    from = 18;
    buffer.GetNbStoredBytes(&to);
    executionStatusCode = buffer.GetBytesFromTo(bytes, to, from);
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    bytes[0] = '\0';
    cout << endl;

    executionStatusCode = buffer.GetBytesFromTo(bytes, from, to);
    bytes[(to - from) + 1] = '\0';
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") = '" << bytes
         << "' ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    bytes[0] = '\0';
    cout << endl;

    buffer.GetNbStoredBytes(&to);
    to += 1;
    executionStatusCode = buffer.GetBytesFromTo(bytes, from, to);
    cout << "buffer.GetBytesFromTo(" << from << ", " << to << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    free(bytes);
    cout << endl;



    // buffer.ReplaceByteAt
    cout << "---------------------------------------------------------" << endl;
    position = miscDYN_BUF_BEGINNING_POSITION - 1;
    byte = 'H';
    executionStatusCode = buffer.ReplaceByteAt(byte, position);
    cout << "buffer.ReplaceByteAt(" << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    position = miscDYN_BUF_BEGINNING_POSITION;
    executionStatusCode = buffer.ReplaceByteAt(byte, position);
    cout << "buffer.ReplaceByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    position = 7;
    byte = 'B';
    executionStatusCode = buffer.ReplaceByteAt(byte, position);
    cout << "buffer.ReplaceByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    position = 13;
    byte = '\'';
    executionStatusCode = buffer.ReplaceByteAt(byte, position);
    cout << "buffer.ReplaceByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    byte = '@';
    executionStatusCode = buffer.ReplaceByteAt(byte, position);
    cout << "buffer.ReplaceByteAt(" << position << ") = '" << byte << "' ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    position += 1;
    executionStatusCode = buffer.ReplaceByteAt(byte, position);
    cout << "buffer.ReplaceByteAt(" << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.InsertBytesAt
    cout << "---------------------------------------------------------" << endl;
    position = miscDYN_BUF_BEGINNING_POSITION - 1;
    executionStatusCode = buffer.InsertBytesAt(NULL, 0, position);
    cout << "buffer.InsertBytesAt(NULL, 0, " << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    position = miscDYN_BUF_BEGINNING_POSITION;
    executionStatusCode = buffer.InsertBytesAt(NULL, 0, position);
    cout << "buffer.InsertBytesAt(NULL, 0, " << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytes = "Encore un '";
    bytesNumber = 0;
    cout << "buffer.InsertBytesAt('" << bytes << "', " << bytesNumber << ", "
         << position << ") ";
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytesNumber = strlen(bytes);
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    cout << "buffer.InsertBytesAt('" << bytes << "', " << bytesNumber << ", "
         << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    position = 18;
    bytes = "misc";
    bytesNumber = 0;
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    cout << "buffer.InsertBytesAt('" << bytes << "', " << bytesNumber << ", "
         << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytesNumber = strlen(bytes);
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    cout << "buffer.InsertBytesAt('" << bytes << "', " << bytesNumber << ", "
         << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    bytes = "~~~";
    bytesNumber = 0;
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    cout << "buffer.InsertBytesAt('" << bytes << "', " << 0 << ", "
         << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    bytesNumber = strlen(bytes);
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    cout << "buffer.InsertBytesAt('" << bytes << "', " << bytesNumber << ", "
         << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    position += 1;
    executionStatusCode = buffer.InsertBytesAt(bytes, bytesNumber,
                                                  position);
    cout << "buffer.InsertBytesAt('" << bytes << "', " << bytesNumber << ", "
         << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.ReplaceBytesFromTo
    cout << "---------------------------------------------------------" << endl;
    from = miscDYN_BUF_BEGINNING_POSITION - 1;
    to   = 9;
    bytes = NULL;
    bytesNumber = 0;
    cout << "buffer.ReplaceBytesFromTo(NULL, " << bytesNumber
         << ", " << from << ", " << to << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes,
                                                       bytesNumber, from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    from = miscDYN_BUF_BEGINNING_POSITION;
    bytes = "Toujours ce";
    bytesNumber = strlen(bytes);
    cout << "buffer.ReplaceBytesFromTo('" << bytes << "', " << bytesNumber
         << ", " << from << ", " << to << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes,
                                                       bytesNumber, from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 40;
    to   = 42;
    bytes = "X";
    bytesNumber = 0;
    cout << "buffer.ReplaceBytesFromTo('" << bytes << "', " << bytesNumber
         << ", " << to << ", " << from << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes, bytesNumber, to,
                                                       from);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    cout << "buffer.ReplaceBytesFromTo('" << bytes << "', " << bytesNumber
         << ", " << from << ", " << to << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes,
                                                       bytesNumber, from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytesNumber = strlen(bytes);
    to   = 40;
    cout << "buffer.ReplaceBytesFromTo('" << bytes << "', " << bytesNumber
         << ", " << from << ", " << to << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes,
                                                       bytesNumber, from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 31;
    buffer.GetNbStoredBytes(&to);
    bytes = " !";
    bytesNumber = strlen(bytes) + 1;
    cout << "buffer.ReplaceBytesFromTo('" << bytes << "', " << bytesNumber
         << ", " << from << ", " << to << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes,
                                                       bytesNumber, from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 31;
    buffer.GetNbStoredBytes(&to);
    to += 1;
    cout << "buffer.ReplaceBytesFromTo('" << bytes << "', " << bytesNumber
         << ", " << from << ", " << to << ") ";
    executionStatusCode = buffer.ReplaceBytesFromTo(bytes,
                                                       bytesNumber, from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.DeleteBytesFromTo
    cout << "---------------------------------------------------------" << endl;
    from = miscDYN_BUF_BEGINNING_POSITION - 1;
    to   = 13;
    cout << "buffer.DeleteBytesFromTo(" << from << ", " << to << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    from = miscDYN_BUF_BEGINNING_POSITION;
    cout << "buffer.DeleteBytesFromTo(" << from << ", " << to << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 7;
    to   = 10;
    cout << "buffer.DeleteBytesFromTo(" << to << ", " << from << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(to, from);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    cout << "buffer.DeleteBytesFromTo(" << from << ", " << to << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 13;
    to   = 13;
    cout << "buffer.DeleteBytesFromTo(" << from << ", " << to << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 13;
    buffer.GetNbStoredBytes(&to);
    to -= 1;
    cout << "buffer.DeleteBytesFromTo(" << from << ", " << to << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 12;
    buffer.GetNbStoredBytes(&to);
    to += 1;
    cout << "buffer.DeleteBytesFromTo(" << from << ", " << to << ") ";
    executionStatusCode = buffer.DeleteBytesFromTo(from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.Strip
    cout << "---------------------------------------------------------" << endl;
    cout << "buffer.Strip() ";
    executionStatusCode = buffer.Strip();
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;



    // buffer.Reset
    cout << "---------------------------------------------------------" << endl;
    cout << "buffer.Reset() ";
    executionStatusCode = buffer.Reset();
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;



    // buffer.AppendString
    cout << "---------------------------------------------------------" << endl;
    cout << "buffer.AppendString(NULL) ";
    executionStatusCode = buffer.AppendString(NULL);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytes = "hello dynStr" ;
    cout << "buffer.AppendString('" << bytes << "') ";
    executionStatusCode = buffer.AppendString(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;

    bytes    = " ... :)" ;
    cout << "buffer.AppendString('" << bytes << "') ";
    executionStatusCode = buffer.AppendString(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;

    bytes    = " !!!" ;
    cout << "buffer.AppendString('" << bytes << "') ";
    executionStatusCode = buffer.AppendString(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;



    // buffer.InsertStringAt
    cout << "---------------------------------------------------------" << endl;
    position = miscDYN_BUF_BEGINNING_POSITION - 1;
    executionStatusCode = buffer.InsertStringAt(NULL, position);
    cout << "buffer.InsertStringAt(NULL, " << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    position = miscDYN_BUF_BEGINNING_POSITION;
    executionStatusCode = buffer.InsertStringAt(NULL, position);
    cout << "buffer.InsertStringAt(NULL, " << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    bytes = "Encore un '";
    executionStatusCode = buffer.InsertStringAt(bytes, position);
    cout << "buffer.InsertStringAt('" << bytes << "', " << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    position = 18;
    bytes = "misc";
    executionStatusCode = buffer.InsertStringAt(bytes, position);
    cout << "buffer.InsertStringAt('" << bytes << "', " << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    bytes = "~~~";
    executionStatusCode = buffer.InsertStringAt(bytes, position);
    cout << "buffer.InsertStringAt('" << bytes << "', " << position << ") ";
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    buffer.GetNbStoredBytes(&position);
    position += 1;
    executionStatusCode = buffer.InsertStringAt(bytes, position);
    cout << "buffer.InsertStringAt('" << bytes << "', " << position << ") ";
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.ReplaceStringFromTo
    cout << "---------------------------------------------------------" << endl;
    from = miscDYN_BUF_BEGINNING_POSITION - 1;
    to   = 9;
    bytes = NULL;
    cout << "buffer.ReplaceStringFromTo(NULL, " << from << ", "
         << to << ") ";
    executionStatusCode = buffer.ReplaceStringFromTo(bytes, from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    from = miscDYN_BUF_BEGINNING_POSITION;
    bytes = "Toujours ce";
    cout << "buffer.ReplaceStringFromTo('" << bytes << ", " << from << ", "
         << to << ") ";
    executionStatusCode = buffer.ReplaceStringFromTo(bytes, from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 40;
    to   = 42;
    bytes = " !";
    cout << "buffer.ReplaceStringFromTo('" << bytes << ", " << to << ", "
         << from << ") ";
    executionStatusCode = buffer.ReplaceStringFromTo(bytes, to, from);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    cout << "buffer.ReplaceStringFromTo('" << bytes << ", " << from << ", "
         << to << ") ";
    executionStatusCode = buffer.ReplaceStringFromTo(bytes, from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;

    from = 31;
    buffer.GetNbStoredBytes(&to);
    cout << "buffer.ReplaceStringFromTo('" << bytes << ", " << from << ", "
         << to << ") ";
    executionStatusCode = buffer.ReplaceStringFromTo(bytes, from, to);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    errCloseStack();
    cout << endl;

    from = 31;
    buffer.GetNbStoredBytes(&to);
    to += 1;
    cout << "buffer.ReplaceStringFromTo('" << bytes << ", " << from << ", "
         << to << ") ";
    executionStatusCode = buffer.ReplaceStringFromTo(bytes, from, to);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << endl;



    // buffer.SetCommentPattern
    cout << "---------------------------------------------------------" << endl;
    bytes = "#";
    cout << "buffer.SetCommentPattern('" << bytes << "') ";
    executionStatusCode = buffer.SetCommentPattern(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << "------------------" << endl;
    bytes = "//";
    cout << "buffer.SetCommentPattern('" << bytes << "') ";
    executionStatusCode = buffer.SetCommentPattern(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << "------------------" << endl;
    bytes = "/**";
    cout << "buffer.SetCommentPattern('" << bytes << "') ";
    executionStatusCode = buffer.SetCommentPattern(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << "------------------" << endl;
    bytes = ";--;";
    cout << "buffer.SetCommentPattern('" << bytes << "') ";
    executionStatusCode = buffer.SetCommentPattern(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << "------------------" << endl;
    bytes = NULL;
    cout << "buffer.SetCommentPattern(NULL) ";
    executionStatusCode = buffer.SetCommentPattern(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << endl;



    // buffer.LoadFile
    cout << "---------------------------------------------------------" << endl;
    bytes = "../doc/";
    cout << "buffer.LoadFile('" << bytes << "') ";
    executionStatusCode = buffer.LoadFile(bytes, NULL);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    bytes = "../doc/moduleDescription.xml";
    cout << "buffer.LoadFile('" << bytes << "') ";
    executionStatusCode = buffer.LoadFile(bytes, NULL);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << endl;



    // buffer.GetNextLine
    cout << "---------------------------------------------------------" << endl;
    mcsSTRING1024 line;
    mcsUINT32     maxLineLength = sizeof(line);
    mcsLOGICAL    skipFlag;
    const char*   ptr = NULL;
    cout << "------------------" << endl;
    ptr = NULL;
    skipFlag = mcsFALSE;
    buffer.SetCommentPattern("\0");
    cout << "skipFlag = '" << (skipFlag == mcsFALSE?"WITHOUT":"WITH")
         << " Comment Skiping' | commentPattern = '"
         << buffer.GetCommentPattern() << "'" << endl;
    while ((ptr = buffer.GetNextLine(ptr, line, maxLineLength, skipFlag))
           != NULL)
    {
        cout << "buffer.GetNextLine() = '" << line << "'" << endl;
    }
    cout << "------------------" << endl;
    ptr = NULL;
    skipFlag = mcsFALSE;
    buffer.SetCommentPattern("*");
    cout << "skipFlag = '" << (skipFlag == mcsFALSE?"WITHOUT":"WITH")
         << " Comment Skiping' | commentPattern = '"
         << buffer.GetCommentPattern() << "'" << endl;
    while ((ptr = buffer.GetNextLine(ptr, line, maxLineLength, skipFlag))
           != NULL)
    {
        cout << "buffer.GetNextLine() = '" << line << "'" << endl;
    }
    cout << "------------------" << endl;
    ptr = NULL;
    skipFlag = mcsTRUE;
    buffer.SetCommentPattern("\0");
    cout << "skipFlag = '" << (skipFlag == mcsFALSE?"WITHOUT":"WITH")
         << " Comment Skiping' | commentPattern = '"
         << buffer.GetCommentPattern() << "'" << endl;
    while ((ptr = buffer.GetNextLine(ptr, line, maxLineLength, skipFlag))
           != NULL)
    {
        cout << "buffer.GetNextLine() = '" << line << "'" << endl;
    }
    cout << "------------------" << endl;
    ptr = NULL;
    skipFlag = mcsTRUE;
    buffer.SetCommentPattern("*");
    cout << "skipFlag = '" << (skipFlag == mcsFALSE?"WITHOUT":"WITH")
         << " Comment Skiping' | commentPattern = '"
         << buffer.GetCommentPattern() << "'" << endl;
    while ((ptr = buffer.GetNextLine(ptr, line, maxLineLength, skipFlag))
           != NULL)
    {
        cout << "buffer.GetNextLine() = '" << line << "'" << endl;
    }
    cout << endl;



    // buffer.GetNextCommentLine
    cout << "---------------------------------------------------------" << endl;
    cout << "------------------" << endl;
    ptr = NULL;
    skipFlag = mcsFALSE;
    buffer.SetCommentPattern("\0");
    cout << "commentPattern = '" << buffer.GetCommentPattern() << "'" << endl;
    while ((ptr = buffer.GetNextCommentLine(ptr, line, maxLineLength)) != NULL)
    {
        cout << "buffer.GetNextCommentLine() = '" << line << "'" << endl;
    }
    cout << "------------------" << endl;
    ptr = NULL;
    skipFlag = mcsFALSE;
    buffer.SetCommentPattern("*");
    cout << "commentPattern = '" << buffer.GetCommentPattern() << "'" << endl;
    while ((ptr = buffer.GetNextCommentLine(ptr, line, maxLineLength)) != NULL)
    {
        cout << "buffer.GetNextCommentLine() = '" << line << "'" << endl;
    }
    cout << endl;



    // buffer.AppendLine
    cout << "---------------------------------------------------------" << endl;
    bytes = NULL;
    cout << "buffer.AppendLine(NULL, 0) ";
    executionStatusCode = buffer.AppendLine(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << "------------------" << endl;
    bytes = "Test de miscAppendLine() !";
    cout << "buffer.AppendLine('" << bytes << "') ";
    executionStatusCode = buffer.AppendLine(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;



    // buffer.AppendCommentLine
    cout << "---------------------------------------------------------" << endl;
    bytes = NULL;
    buffer.SetCommentPattern("");
    cout << "commentPattern = '" << buffer.GetCommentPattern() << "'" << endl
         << "buffer.AppendCommentLine(NULL, 0) ";
    executionStatusCode = buffer.AppendCommentLine(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    cout << "------------------" << endl;
    bytes = "Test de miscAppendCommentLine() !";
    cout << "commentPattern = '" << buffer.GetCommentPattern() << "'" << endl
         << "buffer.AppendCommentLine('" << bytes << "') ";
    executionStatusCode = buffer.AppendCommentLine(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << "------------------" << endl;
    buffer.SetCommentPattern("#");
    cout << "commentPattern = '" << buffer.GetCommentPattern() << "'" << endl
         << "buffer.AppendCommentLine('" << bytes << "') ";
    executionStatusCode = buffer.AppendCommentLine(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << "------------------" << endl;
    buffer.SetCommentPattern(" /*");
    cout << "commentPattern = '" << buffer.GetCommentPattern() << "'" << endl
         << "buffer.AppendCommentLine('" << bytes << "') ";
    executionStatusCode = buffer.AppendCommentLine(bytes);
    displayExecStatus(executionStatusCode);
    errCloseStack();
    buffer.Display();
    cout << endl;



    // buffer.SaveInFile
    cout << "---------------------------------------------------------" << endl;
    bytes = "../tmp/";
    cout << "buffer.SaveInFile('" << bytes << "') ";
    executionStatusCode = buffer.SaveInFile(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    bytes = "../tmp/test.txt";
    cout << "buffer.SaveInFile('" << bytes << "') ";
    executionStatusCode = buffer.SaveInFile(bytes);
    displayExecStatus(executionStatusCode);
    buffer.Display();
    cout << endl;



    cout << "---------------------------------------------------------" << endl
         << "                      THAT'S ALL FOLKS ;)                " << endl
         << "---------------------------------------------------------" << endl;



    // Close MCS services
    mcsExit();
    
    // Exit from the application with SUCCESS
    exit (EXIT_SUCCESS);
}

void displayExecStatus(mcsCOMPL_STAT executionStatusCode)
{
    if (executionStatusCode == mcsFAILURE)
    {
        cout << "FAILED";
        errCloseStack();
    }
    else
    {
        cout << "SUCCEED";
    }

    cout << endl;
    return;
}


/*___oOo___*/
