package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParam;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVO;
import com.atguigu.gmall.search.pojo.SearchResponseVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.*;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVO search(SearchParam searchParam) throws IOException {
        //构建dsl语句
        SearchRequest searchRequest = this.buildQueryDsl(searchParam);
        SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(searchResponse.toString());

        SearchResponseVO responseVO = this.parseSearchResult(searchResponse);
        responseVO.setPageSize(searchParam.getPageSize());
        responseVO.setPageNum(searchParam.getPageNum());
        return responseVO;
    }

    private SearchResponseVO parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVO responseVO = new SearchResponseVO();
        SearchHits hits = searchResponse.getHits();
        //获取总记录数
        responseVO.setTotal(hits.totalHits);
        //解析品牌的聚合结果集
        SearchResponseAttrVO brand = new SearchResponseAttrVO();
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        //注意是ParsedLongTerms
        ParsedLongTerms brandIdAgg = (ParsedLongTerms) aggregationMap.get("brandAgg");
        List<String> brandValues = brandIdAgg.getBuckets().stream().map(bucket -> {
            Map<String, String> map = new HashMap<>();
            //获取品牌的id
            map.put("id", bucket.getKeyAsString());
            //获取品牌的名称，通过子聚合来获取
            Map<String, Aggregation> brandNameMap = bucket.getAggregations().asMap();
            ParsedStringTerms brandNameAgg = (ParsedStringTerms) brandNameMap.get("brandNameAgg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name", brandName);
            return JSON.toJSONString(map);
        }).collect(Collectors.toList());
        brand.setName("品牌");
        brand.setValue(brandValues);
        responseVO.setBrand(brand);
        //解析分类的结果集
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms) aggregationMap.get("categoryIdAgg");
        List<String> categoryValues = categoryIdAgg.getBuckets().stream().map(bucket -> {
            Map<String, String> map = new HashMap<>();
            //获取品牌的id
            map.put("id", bucket.getKeyAsString());
            //获取品牌的名称，通过子聚合来获取
            Map<String, Aggregation> categoryNameMap = bucket.getAggregations().asMap();
            ParsedStringTerms categoryNameAgg = (ParsedStringTerms) categoryNameMap.get("categoryNameAgg");
            String categoryName = categoryNameAgg.getBuckets().get(0).getKeyAsString();
            map.put("name", categoryName);
            return JSON.toJSONString(map);
        }).collect(Collectors.toList());
        SearchResponseAttrVO categoryVO = new SearchResponseAttrVO();
        categoryVO.setValue(categoryValues);
        categoryVO.setName("分类");
        responseVO.setCatelog(categoryVO);

        //构建产品的结果集
        SearchHit[] subHits = hits.getHits();
        List<Goods> goodsList = new ArrayList<>();
        for (SearchHit hit : subHits) {
            Goods goods = JSON.parseObject(hit.getSourceAsString(), Goods.class);
            goods.setTitle(hit.getHighlightFields().get("title").getFragments()[0].toString());
            goodsList.add(goods);
        }
        responseVO.setProducts(goodsList);

        //构建属性的结果集
        //获取嵌套的聚合对象
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        //从嵌套对象中获取规格参数ID的聚合对象
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)) {
            List<SearchResponseAttrVO> attrsVO = buckets.stream().map(bucket -> {
                //构建SearchResponseAttrVO对象
                SearchResponseAttrVO attrVO = new SearchResponseAttrVO();
                //设置规格参数的Id
                attrVO.setProductAttributeId(bucket.getKeyAsNumber().longValue());
                //设置规格参数名
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) (bucket.getAggregations().get("attrNameAgg"));
                List<? extends Terms.Bucket> nameAggBuckets = attrNameAgg.getBuckets();
                attrVO.setName(nameAggBuckets.get(0).getKeyAsString());
                //设置规格参数值
                ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                //stream表达式调用Terms.Bucket中的方法
                List<String> values = valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                attrVO.setValue(values);
                return attrVO;
            }).collect(Collectors.toList());
            //属性结果
            responseVO.setAttrs(attrsVO);
        }
        return responseVO;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //查询关键字
        String keyword = searchParam.getKeyword();
        if (StringUtils.isEmpty(keyword)) {
            return null;
        }
        //查询条件的构建器
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //1.构建查询条件和过滤的条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //1.1构建查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        //1.2构建过滤条件
        //1.2.1  构建品牌的过滤条件
        String[] brand = searchParam.getBrand();
        if (brand != null && brand.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brand));
        }
        //1.2.2  构建分类的过滤条件
        String[] catelog3 = searchParam.getCatelog3();
        if (catelog3 != null && catelog3.length != 0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", catelog3));
        }
        //1.2.3 构建规格属性嵌套过滤
        String[] props = searchParam.getProps();
        if (props != null && props.length != 0) {
            for (String prop : props) {
                //以 : 进行分隔，分隔后应该是两个元素，1-attrId 2-attrValue
                String[] split = StringUtils.split(prop, ":");
                if (split == null || split.length != 2) {
                    continue;
                }
                //以-分割处理出AttrValues
                String[] attrValues = StringUtils.split(split[1], "-");
                //构造嵌套查询
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //构造嵌套查询中的子查询
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                //构建子查询中的过滤条件
                subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                subBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));

                //把嵌套查询放入到过滤器中
                boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));
                boolQueryBuilder.filter(boolQuery);
            }
        }
        //1.2.4 价格区间的过滤
        RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
        Integer priceFrom = searchParam.getPriceFrom();
        Integer priceTo = searchParam.getPriceTo();
        if (priceFrom != null) {
            rangeQuery.gte(priceFrom);
        }
        if (priceTo != null) {
            rangeQuery.lte(priceTo);
        }
        boolQueryBuilder.filter(rangeQuery);

        sourceBuilder.query(boolQueryBuilder);

        //2.构建分页
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);

        //3.构建排序
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)) {
            String[] split = StringUtils.split(order, ":");
            if (split != null && split.length == 2) {
                String field = null;
                switch (split[0]) {
                    case "1":
                        field = "sale";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                sourceBuilder.sort(field, StringUtils.equals("asc", split[1]) ? SortOrder.ASC : SortOrder.DESC);
            }
        }
        //4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder().field("title").preTags("<em>").postTags("</em>"));

        //5.构建聚合
        //5.1品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brandAgg").field("brandId").subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"));
        sourceBuilder.aggregation(brandAgg);
        //5.2分类的聚合
        TermsAggregationBuilder categoryIdAgg = AggregationBuilders.terms("categoryIdAgg").field("categoryId").subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName"));
        sourceBuilder.aggregation(categoryIdAgg);

        //5.3搜索的规格属性聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attrAgg", "attrs").
                subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").
                        subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName")).
                        subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue")));
        sourceBuilder.aggregation(attrAgg);

        //结果集的过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "pic", "title", "price"}, null);

        //查询参数
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(sourceBuilder);
        return searchRequest;
    }

}
