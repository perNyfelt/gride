package se.alipsa.grade.code.mdtab;

import org.fxmisc.flowless.VirtualizedScrollPane;
import se.alipsa.grade.Grade;
import se.alipsa.grade.code.CodeTextArea;
import se.alipsa.grade.code.CodeType;
import se.alipsa.grade.code.TextAreaTab;

import java.io.File;

public class MdTab extends TextAreaTab  {

  private final MdTextArea mdTextArea;

  public MdTab(String title, Grade gui) {
    super(gui, CodeType.TXT);
    setTitle(title);
    mdTextArea = new MdTextArea(this);
    VirtualizedScrollPane<MdTextArea> txtPane = new VirtualizedScrollPane<>(mdTextArea);
    pane.setCenter(txtPane);
  }

  @Override
  public CodeTextArea getCodeArea() {
    return mdTextArea;
  }

  @Override
  public File getFile() {
    return mdTextArea.getFile();
  }

  @Override
  public void setFile(File file) {
    mdTextArea.setFile(file);
  }

  @Override
  public String getTextContent() {
    return mdTextArea.getTextContent();
  }

  @Override
  public String getAllTextContent() {
    return mdTextArea.getAllTextContent();
  }

  @Override
  public void replaceContentText(int start, int end, String content) {
    mdTextArea.replaceContentText(start, end, content);
  }

  @Override
  public void replaceContentText(String content, boolean isReadFromFile) {
    mdTextArea.replaceContentText(content, isReadFromFile);
  }
}
