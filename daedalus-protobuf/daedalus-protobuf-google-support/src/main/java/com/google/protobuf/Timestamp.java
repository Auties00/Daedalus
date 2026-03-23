package com.google.protobuf;

import it.auties.protobuf.annotation.ProtobufMessage;

/**
 * A Timestamp represents a point in time independent of any time zone or local
 * calendar, encoded as a count of seconds and fractions of seconds at
 * nanosecond resolution. The count is relative to an epoch at UTC midnight on
 * January 1, 1970, in the proleptic Gregorian calendar which extends the
 * Gregorian calendar backwards to year one.
 *
 * <p>All minutes are 60 seconds long. Leap seconds are "smeared" so that no leap
 * second table is needed for interpretation, using a [24-hour linear
 * smear](<a href="https://developers.google.com/time/smear">https://developers.google.com/time/smear</a>).
 *
 * <p>The range is from 0001-01-01T00:00:00Z to 9999-12-31T23:59:59.999999999Z. By
 * restricting to that range, we ensure that we can convert to and from [RFC
 * 3339](<a href="https://www.ietf.org/rfc/rfc3339.txt">https://www.ietf.org/rfc/rfc3339.txt</a>) date strings.
 *
 * <h3>Examples</h3>
 *
 *
 * <p>Example 1: Compute Timestamp from POSIX {@code time()}.
 *
 * <pre>
 *   Timestamp timestamp;
 *   timestamp.set_seconds(time(NULL));
 *   timestamp.set_nanos(0);
 * </pre>
 *
 * <p>Example 2: Compute Timestamp from POSIX {@code gettimeofday()}.
 *
 * <pre>
 *   struct timeval tv;
 *   gettimeofday(&amp;tv, NULL);
 *
 *   Timestamp timestamp;
 *   timestamp.set_seconds(tv.tv_sec);
 *   timestamp.set_nanos(tv.tv_usec * 1000);
 * </pre>
 *
 * <p>Example 3: Compute Timestamp from Win32 {@code GetSystemTimeAsFileTime()}.
 *
 * <pre>
 *   FILETIME ft;
 *   GetSystemTimeAsFileTime(&amp;ft);
 *   UINT64 ticks = (((UINT64)ft.dwHighDateTime) &lt;&lt; 32) | ft.dwLowDateTime;
 *
 *   // A Windows tick is 100 nanoseconds. Windows epoch 1601-01-01T00:00:00Z
 *   // is 11644473600 seconds before Unix epoch 1970-01-01T00:00:00Z.
 *   Timestamp timestamp;
 *   timestamp.set_seconds((INT64) ((ticks / 10000000) - 11644473600LL));
 *   timestamp.set_nanos((INT32) ((ticks % 10000000) * 100));
 * </pre>
 *
 * <p>Example 4: Compute Timestamp from Java {@code System.currentTimeMillis()}.
 *
 * <pre>
 *   long millis = System.currentTimeMillis();
 *
 *   Timestamp timestamp = Timestamp.newBuilder().setSeconds(millis / 1000)
 *       .setNanos((int) ((millis % 1000) * 1000000)).build();
 * </pre>
 *
 * <p>Example 5: Compute Timestamp from Java {@code Instant.now()}.
 *
 * <pre>
 *   Instant now = Instant.now();
 *
 *   Timestamp timestamp =
 *       Timestamp.newBuilder().setSeconds(now.getEpochSecond())
 *           .setNanos(now.getNano()).build();
 * </pre>
 *
 * <p>Example 6: Compute Timestamp from current time in Python.
 *
 * <pre>
 *   timestamp = Timestamp()
 *   timestamp.GetCurrentTime()
 * </pre>
 *
 * <h3>JSON Mapping</h3>
 *
 *
 * <p>In JSON format, the Timestamp type is encoded as a string in the
 * <a href="https://www.ietf.org/rfc/rfc3339.txt">RFC 3339</a> format. That is, the
 * format is "{year}-{month}-{day}T{hour}:{min}:{sec}[.{frac_sec}]Z"
 * where {year} is always expressed using four digits while {month}, {day},
 * {hour}, {min}, and {sec} are zero-padded to two digits each. The fractional
 * seconds, which can go up to 9 digits (i.e. up to 1 nanosecond resolution),
 * are optional. The "Z" suffix indicates the timezone ("UTC"); the timezone
 * is required. A ProtoJSON serializer should always use UTC (as indicated by
 * "Z") when printing the Timestamp type and a ProtoJSON parser should be
 * able to accept both UTC and other timezones (as indicated by an offset).
 *
 * <p>For example, "2017-01-15T01:30:15.01Z" encodes 15.01 seconds past
 * 01:30 UTC on January 15, 2017.
 *
 * <p>In JavaScript, one can convert a Date object to this format using the
 * standard
 * <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date/toISOString">toISOString()</a>
 * method. In Python, a standard {@code datetime.datetime} object can be converted
 * to this format using
 * <a href="https://docs.python.org/2/library/time.html#time.strftime">strftime</a> with
 * the time format spec '%Y-%m-%dT%H:%M:%S.%fZ'. Likewise, in Java, one can use
 * the Joda Time's [{@code ISODateTimeFormat.dateTime()}](
 * <a href="http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTime(">http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISODateTimeFormat.html#dateTime(</a>)
 * ) to obtain a formatter capable of generating timestamps in this format.
 *
 * <p>Protobuf type {@code google.protobuf.Timestamp}
 */
@ProtobufMessage
public final class Timestamp {

    /**
     * Represents seconds of UTC time since Unix epoch 1970-01-01T00:00:00Z. Must
     * be between -62135596800 and 253402300799 inclusive (which corresponds to
     * 0001-01-01T00:00:00Z to 9999-12-31T23:59:59Z).
     *
     * <p><code>int64 seconds = 1;</code>
     */
    @ProtobufMessage.Int64Field(index = 1)
    long seconds;

    /**
     * Non-negative fractions of a second at nanosecond resolution. This field is
     * the nanosecond portion of the duration, not an alternative to seconds.
     * Negative second values with fractions must still have non-negative nanos
     * values that count forward in time. Must be between 0 and 999,999,999
     * inclusive.
     *
     * <p><code>int32 nanos = 2;</code>
     */
    @ProtobufMessage.Int32Field(index = 2)
    int nanos;

    Timestamp(long seconds, int nanos) {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    public long seconds() {
        return seconds;
    }

    public int nanos() {
        return nanos;
    }
}
