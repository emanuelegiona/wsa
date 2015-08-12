package wsa.web.html;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class ParsedHTML implements Parsed{
    private ParseNode root;

    public class ParseNode extends Parsed.Node {
        public List<ParseNode> children;

        public ParseNode(String t, Map<String, String> a, String c) {
            super(t, a, c);
            children = new ArrayList<>();
        }

        public ParseNode(String t, Map<String, String> a, String c, List<ParseNode> children) {
            super(t, a, c);
            this.children = children;
        }

        public ParseNode parse(org.w3c.dom.Node node){
            ParseNode ret=null;
            if(node!=null) {
                String tag=node.getNodeName();

                Map<String, String> attrs = null;
                NamedNodeMap nAttrs = node.getAttributes();
                if (nAttrs != null && nAttrs.getLength() > 0) {
                    attrs = new HashMap<>();
                    for (int i = 0; i < nAttrs.getLength(); i++) {
                        String url = nAttrs.item(i).getNodeValue();
                        attrs.put(nAttrs.item(i).getNodeName(), url);
                    }
                }

                String content = null;
                if (node.getNodeType()!=org.w3c.dom.Node.ELEMENT_NODE)
                    content = node.getNodeValue();

                NodeList nChildren = node.getChildNodes();
                List<ParseNode> children = new ArrayList<>();
                ret = new ParseNode(tag, attrs, content, children);
                if (nChildren != null && nChildren.getLength() > 0) {
                    for (int i = 0; i < nChildren.getLength(); i++)
                        children.add(parse(nChildren.item(i)));
                }
                ret.children = children;
            }
            return ret;
        }

        public Node toNode(){
            return new Node(this.tag,this.attr,this.content);
        }

        public void apply(Consumer<Node> visitor){
            visitor.accept(root);
            if(root.children!=null && root.children.size()>0)
                for(ParseNode c:root.children)
                    c.apply(visitor);
        }

        public List<ParseNode> findByTag(String tag){
            List<ParseNode> nodes=new ArrayList<>();
            if(this.tag.equals(tag.toLowerCase()) || this.tag.equals(tag.toUpperCase()))
                nodes.add(this);
            if(this.children!=null && this.children.size()>0)
                for(ParseNode c:this.children)
                    nodes.addAll(c.findByTag(tag));
            return nodes;
        }
    }

    public ParsedHTML(Document doc){
        if(doc!=null) {
            org.w3c.dom.Node docRoot=null;
            for(int i=0;i<doc.getChildNodes().getLength() && docRoot==null;i++)
                if(doc.getChildNodes().item(i).getNodeType()==org.w3c.dom.Node.ELEMENT_NODE)
                    docRoot=doc.getChildNodes().item(i);
            root = new ParseNode(null, null, null).parse(docRoot);
        }
        else
            root=null;
    }

    /**Esegue la visita dell'intero albero di parsing
     * @param visitor visitatore invocato su ogni nodo dell'albero*/
    @Override
    public void visit(Consumer<Node> visitor) {
        root.apply(visitor);
    }

    /**Ritorna la lista (possibilmente vuota) dei links contenuti nella pagina
     *@return la lista dei links (mai null)*/
    @Override
    public List<String> getLinks() {
        List<Node> nodes=getByTag("a");
        List<String> links=new ArrayList<>();
        for(Node n:nodes) {
            if(n!=null && n.attr!=null && n.attr.get("href")!=null)
                if(!n.attr.get("href").contains("mailto:"))
                    links.add(n.attr.get("href"));
        }
        return links;
    }

    /**Ritorna la lista (possibilmente vuota) dei nodi con lo specificato tag
     * @param tag un nome di tag
     * @return la lista dei nodi con il dato tag (mai null)*/
    @Override
    public List<Node> getByTag(String tag) {
        return createList(ParseNode::toNode,root.findByTag(tag));
    }

    public ParseNode getRoot(Document doc){
        return root;
    }

    private List<Node> createList(Function<ParseNode,Node> func,List<ParseNode> src){
        List<Node> nodes=new ArrayList<>();
        for(ParseNode p:src)
            nodes.add(func.apply(p));
        return nodes;
    }
}
