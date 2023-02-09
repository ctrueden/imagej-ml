package net.imagej.ml;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowFileReader;
import org.apache.arrow.vector.ipc.ArrowFileWriter;
import org.apache.arrow.vector.ipc.message.ArrowBlock;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * TODO
 *
 * @author Curtis Rueden
 */
public class IPC {

	/*
	private static void arrowWrite(Path p) {
		List<Field> children = null;
		Field age = new Field("age",
			FieldType.nullable(new ArrowType.Int(32, true)),
			children);
		Field name = new Field("name",
			FieldType.nullable(new ArrowType.Utf8()),
			children);
		Schema schema = new Schema(asList(age, name));
		try(
				BufferAllocator allocator = new RootAllocator();
				VectorSchemaRoot root = VectorSchemaRoot.create(schema, allocator);
				IntVector ageVector = (IntVector) root.getVector("age");
				VarCharVector nameVector = (VarCharVector) root.getVector("name");
				){
			ageVector.allocateNew(3);
			ageVector.set(0, 10);
			ageVector.set(1, 20);
			ageVector.set(2, 30);
			nameVector.allocateNew(3);
			nameVector.set(0, "Dave".getBytes(StandardCharsets.UTF_8));
			nameVector.set(1, "Peter".getBytes(StandardCharsets.UTF_8));
			nameVector.set(2, "Mary".getBytes(StandardCharsets.UTF_8));
			root.setRowCount(3);
			File file = p.toFile();
			DictionaryProvider provider = null;
			try (
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				ArrowFileWriter writer = new ArrowFileWriter(root, provider, fileOutputStream.getChannel());
			) {
				writer.start();
				writer.writeBatch();
				writer.end();
				System.out.println("Record batches written: " + writer.getRecordBlocks().size()
					+ ". Number of rows written: " + root.getRowCount());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}

	private static List<String> arrowRead(Path p) throws IOException {
		try (//
				BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
				FileInputStream fileInputStream = new FileInputStream(p.toFile());
				ArrowFileReader reader = //
					new ArrowFileReader(fileInputStream.getChannel(), allocator)//
		) {
			System.out.println("Record batches in file: " + reader.getRecordBlocks()
				.size());
			List<String> results = new ArrayList<>();
			for (ArrowBlock arrowBlock : reader.getRecordBlocks()) {
				reader.loadRecordBatch(arrowBlock);
				String data = reader.getVectorSchemaRoot().contentToTSVString();
				results.add(data);
			}
			return results;
		}
		finally {}
	}
	*/

	private static void startPolling(MappedByteBuffer buf) {
		byte[] b = new byte[buf.limit()];
		Thread t = new Thread(() -> {
			while (true) {
				buf.position(0);
				buf.get(b);
				System.out.println(Arrays.toString(b));
				try { Thread.sleep(500); }
				catch (InterruptedException exc) {
					exc.printStackTrace();
				}
			}
		});
		t.setName("Poller");
		t.setDaemon(true);
		t.start();
	}

	public static void main(final String... args) throws Exception {
		//Path tempDir = Files.createTempDirectory("curtis");
		//System.out.println(tempDir);
		// on macos, outputs something of the form:
		// /var/folders/n2/5q09bjq11kv6xwpp0xlg4npc0000gq/T/curtis7281232030776860312
		Path userHome = Paths.get(System.getProperty("user.home"));
		Path ram = userHome.resolve("ram");

		/*
		Path ipc = ram.resolve("ipc");
		arrowWrite(ipc);
		arrowRead(ipc);
		*/

		Path p = ram.resolve("buffer");
		try (FileChannel fc = FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
			int size = 64;
			MappedByteBuffer buf = fc.map(MapMode.READ_WRITE, 0, size);
			buf.put(new byte[size]);
			startPolling(buf);
			for (int i=0; i<1000000; i++) {
				int index = i % size;
				byte value = (byte) i;
				if (i < 10) buf.put(index, value);
				System.out.println("WRITER: buf[" + index + "] -> " + value);
				Thread.sleep(1000);
			}
		}
		finally {
			Files.deleteIfExists(p);
			System.out.println("Done");
		}
	}

}
