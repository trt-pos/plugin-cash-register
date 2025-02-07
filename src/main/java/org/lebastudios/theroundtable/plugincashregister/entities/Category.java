package org.lebastudios.theroundtable.plugincashregister.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.lebastudios.theroundtable.database.Database;
import org.lebastudios.theroundtable.plugincashregister.PluginCashRegisterEvents;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pr_category")
public class Category
{
    static
    {
        PluginCashRegisterEvents.onProductModify.addListener(_ ->
        {
            // Delete categories and subcategories that have no products.
            Database.getInstance().connectTransaction(session ->
            {
                var categories = session.createQuery("from Category", Category.class)
                        .getResultList();

                categories.forEach(category ->
                {
                    boolean conservesSomeSubcategory = false;
                    var list = new ArrayList<>(category.getSubCategories());
                    for (var subcategories : list)
                    {
                        if (subcategories.getProducts().isEmpty())
                        {
                            session.remove(subcategories);
                        }
                        else
                        {
                            conservesSomeSubcategory = true;
                        }
                    }

                    if (!conservesSomeSubcategory) session.remove(category);
                });
            });
        });
    }

    @Id
    @Column(name = "name")
    private String name;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "category", cascade = CascadeType.REMOVE)
    private Set<SubCategory> subCategories;
    
    @Override
    public int hashCode()
    {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Category category)) return false;

        return Objects.equals(name, category.name);
    }
}
